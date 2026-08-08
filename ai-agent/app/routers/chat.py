from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from app.models.schemas import ChatRequest
from app.services.rag_service import rag_chat, rag_chat_stream
import logging

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/chat")
def chat(req: ChatRequest):
    """非流式聊天，返回 answer + should_save 标记"""
    try:
        result = rag_chat(req.user_id, req.question, req.note_id, req.history, req.mode)
        return result
    except Exception as e:
        logger.error(f"Chat error: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/chat/stream")
def chat_stream(req: ChatRequest):
    """流式聊天（SSE），末尾 [DONE] 行附带 should_save 元数据"""
    def generate():
        try:
            for token in rag_chat_stream(req.user_id, req.question, req.note_id, req.history, req.mode):
                yield f"data: {token}\n\n"
        except Exception as e:
            logger.error(f"Stream error: {str(e)}", exc_info=True)
            yield f"data: [ERROR] {str(e)}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
