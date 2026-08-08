from fastapi import APIRouter, HTTPException
from app.models.schemas import SyncEmbeddingRequest, SearchNotesRequest
from app.services.embedding_service import sync_note_embedding, delete_note_embedding, search_by_vector

router = APIRouter()


@router.post("/embeddings/sync")
def sync(req: SyncEmbeddingRequest):
    try:
        sync_note_embedding(req.note_id, req.user_id, req.content, req.title)
        return {"status": "ok"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/embeddings/{note_id}")
def delete(note_id: int):
    try:
        delete_note_embedding(note_id)
        return {"status": "ok"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/search-notes")
def search(req: SearchNotesRequest):
    try:
        results = search_by_vector(req.query, req.user_id, req.top_k)
        return {"results": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
