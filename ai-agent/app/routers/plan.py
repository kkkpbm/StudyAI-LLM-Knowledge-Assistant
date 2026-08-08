from fastapi import APIRouter, HTTPException
from app.models.schemas import PlanRequest
from app.services.plan_service import generate_learning_plan

router = APIRouter()


@router.post("/gen-learning-plan")
def gen_learning_plan(req: PlanRequest):
    try:
        plan = generate_learning_plan(req.goal)
        return plan
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
