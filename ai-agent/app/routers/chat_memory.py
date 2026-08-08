from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.chat_memory_service import sync_chat_message, search_chat_memory

router = APIRouter()


class SyncChatRequest(BaseModel):
    user_id: int
    question: str
    answer: str


class SearchChatRequest(BaseModel):
    user_id: int
    query: str
    top_k: int = 5


@router.post("/chat-memory/sync")
def sync(req: SyncChatRequest):
    try:
        sync_chat_message(req.user_id, req.question, req.answer)
        return {"status": "ok"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/chat-memory/search")
def search(req: SearchChatRequest):
    try:
        results = search_chat_memory(req.user_id, req.query, req.top_k)
        return {"results": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
