from fastapi import APIRouter, HTTPException
from app.models.schemas import ReviewRequest, ReviewResponse
from app.services.review_service import calculate_next_review

router = APIRouter()


@router.post("/next-review")
def next_review(req: ReviewRequest):
    try:
        interval, ef, _ = calculate_next_review(req.quality, req.interval_days, req.ease_factor)
        return ReviewResponse(next_interval_days=interval, ease_factor=ef)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
