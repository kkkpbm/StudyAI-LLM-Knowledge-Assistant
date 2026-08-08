"""聊天记忆服务：将对话存入向量知识库，支持语义检索历史对话"""
from app.services.vector_store import get_collection

CHAT_USER_PREFIX = "chat_user_"


def _make_doc_id(user_id: int, chat_index: int) -> str:
    return f"{CHAT_USER_PREFIX}{user_id}_{chat_index}"


def sync_chat_message(user_id: int, question: str, answer: str) -> None:
    """将一轮对话存入向量库"""
    collection = get_collection()
    # 用时间戳作为序号避免冲突
    import time
    idx = int(time.time() * 1000)
    doc_text = f"用户提问: {question}\nAI回答: {answer}"

    collection.upsert(
        ids=[_make_doc_id(user_id, idx)],
        documents=[doc_text],
        metadatas=[{
            "user_id": str(user_id),
            "type": "chat_memory",
            "question": question[:200],
        }],
    )


def search_chat_memory(user_id: int, query: str, top_k: int = 5) -> list[dict]:
    """搜索相关的历史对话"""
    collection = get_collection()
    try:
        results = collection.query(
            query_texts=[query],
            n_results=top_k,
            where={"user_id": str(user_id)},
            include=["documents", "metadatas", "distances"],
        )
    except Exception:
        return []

    ids = results.get("ids", [[]])[0]
    docs = results.get("documents", [[]])[0]
    metas = results.get("metadatas", [[]])[0]
    distances = results.get("distances", [[]])[0]

    items = []
    for i in range(len(ids)):
        items.append({
            "id": ids[i],
            "content": docs[i][:800] if docs[i] else "",
            "question": metas[i].get("question", "") if metas[i] else "",
            "score": float(1 - (distances[i] if distances else 0)),
        })
    return items
