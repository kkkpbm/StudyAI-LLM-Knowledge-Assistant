from concurrent.futures import ThreadPoolExecutor
from langchain_core.prompts import ChatPromptTemplate
from app.services.llm_service import llm, streaming_llm
from app.services.embedding_service import search_by_vector
from app.services.chat_memory_service import search_chat_memory
import logging
from time import perf_counter
from uuid import uuid4

logger = logging.getLogger(__name__)

RAG_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """You are a helpful AI assistant with memory. Answer questions based on the user's knowledge base, past conversations, and recent chat history.

## Core rules
1. Use the retrieved context for factual accuracy. Use conversation history to maintain continuity.
2. If the context doesn't contain relevant info, say so and provide a general answer.
3. **IMPORTANT — Clarify before answering**: If the user's question is vague or broad (e.g. "帮我学Python", "推荐书", "怎么提升自己"), do NOT give a generic answer. Instead, ask 1-2 clarifying questions to narrow down their real need. For example: "你想学Python的哪个方向？数据分析、Web开发还是自动化？目前基础怎么样？" Only answer directly when the question is specific enough.
4. Keep responses concise. Use Chinese."""),
    ("human", "Conversation history:\n{history}\n\nKnowledge context:\n{context}\n\nQuestion: {question}"),
])

DIRECT_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """You are a helpful AI assistant. Remember what was discussed earlier. Answer clearly and concisely. Use Chinese.

IMPORTANT: If the question is too vague or broad, ask clarifying questions instead of giving a generic answer."""),
    ("human", "Conversation history:\n{history}\n\nQuestion: {question}"),
])


def _format_history(history: list[dict]) -> str:
    """将前端传来的对话历史格式化为文本"""
    if not history:
        logger.info("chat history received: EMPTY")
        return "（无历史对话）"
    lines = []
    for msg in history[-10:]:  # 最多取最近 10 条
        role = "用户" if msg.get("role") == "user" else "助手"
        content = msg.get("content", "")
        if content:
            lines.append(f"{role}: {content[:300]}")
    result = "\n".join(lines) if lines else "（无历史对话）"
    logger.info(f"chat history received: {len(history)} messages, formatted {len(lines)} lines")
    return result


def _build_context(user_id: int, question: str, note_id: int | None = None, trace_id: str = "-") -> tuple[str, bool, list[dict]]:
    """并行检索笔记和聊天记忆，构建上下文。返回 (context, has_notes)。"""
    context_parts = []

    note_results = []
    chat_results = []
    def timed_search(name, search_fn, *args):
        started_at = perf_counter()
        try:
            return search_fn(*args)
        finally:
            logger.info("ai_timing trace=%s stage=%s elapsed_ms=%d", trace_id, name,
                        (perf_counter() - started_at) * 1000)

    context_started_at = perf_counter()
    with ThreadPoolExecutor(max_workers=2) as executor:
        note_future = executor.submit(timed_search, "note_vector_search", search_by_vector, question, user_id, 3)
        chat_future = executor.submit(timed_search, "memory_vector_search", search_chat_memory, user_id, question, 3)
        try:
            note_results = note_future.result(timeout=30)
        except Exception:
            logger.warning("Vector search for notes timed out, continuing without notes context")
        try:
            chat_results = chat_future.result(timeout=30)
        except Exception:
            logger.warning("Vector search for chat memory timed out")

    if note_id is not None:
        try:
            all_notes = timed_search("note_filter_search", search_by_vector, question, user_id, 10)
            filtered = [r for r in all_notes if str(r.get("note_id")) == str(note_id)]
            if filtered:
                note_results = filtered + [r for r in note_results if r not in filtered]
        except Exception:
            pass

    has_notes = False
    sources = []
    for i, result in enumerate(note_results[:3]):
        if result.get("content"):
            has_notes = True
            sources.append({
                "noteId": result.get("note_id"),
                "title": result.get("title") or f"笔记 #{result.get('note_id')}",
                "snippet": result.get("content", "")[:180],
                "score": round(float(result.get("score", 0)), 4),
            })
            context_parts.append(f"[笔记{i+1}]: {result['content'][:500]}")

    for i, result in enumerate(chat_results[:3]):
        if result.get("content"):
            context_parts.append(f"[历史对话{i+1}]: {result['content'][:300]}")

    logger.info("ai_timing trace=%s stage=context_build elapsed_ms=%d note_results=%d memory_results=%d",
                trace_id, (perf_counter() - context_started_at) * 1000, len(note_results), len(chat_results))
    return ("\n\n".join(context_parts) if context_parts else "", has_notes, sources)


def rag_chat(user_id: int, question: str, note_id: int | None = None, history: list[dict] | None = None,
             mode: str = "knowledge") -> dict:
    """非流式聊天，返回 {answer, should_save, suggested_title}"""
    if history is None:
        history = []
    try:
        trace_id = uuid4().hex[:10]
        started_at = perf_counter()
        knowledge_mode = mode == "knowledge"
        context, has_notes, sources = _build_context(user_id, question, note_id, trace_id) if knowledge_mode else ("", False, [])
        if not knowledge_mode:
            logger.info("ai_timing trace=%s stage=retrieval skipped=true mode=chat", trace_id)
        history_text = _format_history(history)

        if context:
            chain = RAG_PROMPT | llm
            response = chain.invoke({"history": history_text, "context": context, "question": question})
            answer = response.content
        else:
            chain = DIRECT_PROMPT | llm
            response = chain.invoke({"history": history_text, "question": question})
            answer = response.content

        return {
            "answer": answer,
            "should_save": knowledge_mode and not has_notes,
            "suggested_title": question[:50],
            "sources": sources,
        }
    except Exception as e:
        logger.error(f"RAG chat error: {str(e)}", exc_info=True)
        return {
            "answer": "抱歉，AI 服务暂时不可用，请稍后再试。",
            "should_save": False,
            "suggested_title": "",
        }


def rag_chat_stream(user_id: int, question: str, note_id: int | None = None, history: list[dict] | None = None,
                    mode: str = "knowledge"):
    """流式聊天生成器，逐 token yield，末尾 yield sentinel 字符串包含元数据"""
    if history is None:
        history = []
    trace_id = uuid4().hex[:10]
    request_started_at = perf_counter()
    knowledge_mode = mode == "knowledge"
    has_notes = False
    sources = []
    first_token_logged = False
    try:
        context, has_notes, sources = _build_context(user_id, question, note_id, trace_id) if knowledge_mode else ("", False, [])
        if not knowledge_mode:
            logger.info("ai_timing trace=%s stage=retrieval skipped=true mode=chat", trace_id)
        history_text = _format_history(history)
        model_started_at = perf_counter()

        if knowledge_mode and context:
            chain = RAG_PROMPT | streaming_llm
            for chunk in chain.stream({"history": history_text, "context": context, "question": question}):
                if chunk.content:
                    if not first_token_logged:
                        first_token_logged = True
                        logger.info("ai_timing trace=%s stage=deepseek_first_token elapsed_ms=%d total_ms=%d mode=%s",
                                    trace_id, (perf_counter() - model_started_at) * 1000,
                                    (perf_counter() - request_started_at) * 1000, mode)
                    yield chunk.content
        else:
            chain = DIRECT_PROMPT | streaming_llm
            for chunk in chain.stream({"history": history_text, "question": question}):
                if chunk.content:
                    if not first_token_logged:
                        first_token_logged = True
                        logger.info("ai_timing trace=%s stage=deepseek_first_token elapsed_ms=%d total_ms=%d mode=%s",
                                    trace_id, (perf_counter() - model_started_at) * 1000,
                                    (perf_counter() - request_started_at) * 1000, mode)
                    yield chunk.content
    except Exception as e:
        logger.error(f"RAG stream error: {str(e)}", exc_info=True)
        yield "抱歉，AI 服务暂时不可用，请稍后再试。"

    import json
    meta = {"should_save": knowledge_mode and not has_notes, "suggested_title": question[:50], "sources": sources}
    logger.info("ai_timing trace=%s stage=stream_completed total_ms=%d mode=%s source_count=%d",
                trace_id, (perf_counter() - request_started_at) * 1000, mode, len(sources))
    yield "[DONE]" + json.dumps(meta, ensure_ascii=False)
