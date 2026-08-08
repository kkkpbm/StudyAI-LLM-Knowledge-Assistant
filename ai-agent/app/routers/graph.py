from fastapi import APIRouter, HTTPException
from app.models.schemas import ExtractGraphRequest
from app.services.graph_service import extract_knowledge_graph

router = APIRouter()


@router.post("/extract-graph")
def extract_graph(req: ExtractGraphRequest):
    try:
        relations = extract_knowledge_graph(req.content)
        return {"relations": relations}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
