from pydantic import BaseModel


class SummarizeRequest(BaseModel):
    content: str


class ChatRequest(BaseModel):
    user_id: int
    note_id: int | None = None
    question: str
    history: list[dict] = []
    # knowledge: 检索个人笔记与长期对话记忆；chat: 仅基于当前会话直接回答
    mode: str = "knowledge"


class ExtractGraphRequest(BaseModel):
    content: str


class PlanRequest(BaseModel):
    goal: str


class AssessRequest(BaseModel):
    content: str


class SyncEmbeddingRequest(BaseModel):
    note_id: int
    user_id: int
    content: str
    title: str = ""


class SearchNotesRequest(BaseModel):
    user_id: int
    query: str
    top_k: int = 5


class ReviewRequest(BaseModel):
    user_id: int
    note_id: int
    quality: int  # 0-5
    interval_days: int = 1
    ease_factor: float = 2.5


class ReviewResponse(BaseModel):
    next_interval_days: int
    ease_factor: float

class ChatResponse(BaseModel):
    answer: str
    should_save: bool = False
    suggested_title: str = ""


class FlashcardGenerateRequest(BaseModel):
    title: str = ""
    content: str
    count: int = 6
