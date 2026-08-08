from fastapi import APIRouter, HTTPException
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel
from app.services.llm_service import llm

router = APIRouter()

INSIGHT_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """You are a learning coach. Analyze the user's learning data and give personalized advice.
Reply with ONLY a valid JSON object, no other text.

Format:
- summary: a 2-3 sentence summary of the user's current learning status (in Chinese)
- strengths: array of 1-2 things they're doing well
- weaknesses: array of 1-2 areas to improve
- review_today: array of topics they should review today (based on the review data)
- next_step: a specific recommendation on what to learn next (1 sentence in Chinese)
- motivation: a brief encouraging message in Chinese"""),
    ("human", """Learning data:
- Total notes: {note_count}
- Active learning plans: {plan_count}
- Today study minutes: {today_minutes}
- Knowledge concepts: {concept_count}
- Reviews due today: {review_count}
- Upcoming reviews (next 3 days): {upcoming_reviews}
- Recent study topics: {recent_topics}
- Category distribution: {categories}"""),
])


class InsightRequest(BaseModel):
    note_count: int = 0
    plan_count: int = 0
    today_minutes: int = 0
    concept_count: int = 0
    review_count: int = 0
    upcoming_reviews: str = ""
    recent_topics: str = ""
    categories: str = ""


@router.post("/learning-insight")
def learning_insight(req: InsightRequest):
    """分析学习数据，给出个性化建议"""
    try:
        chain = INSIGHT_PROMPT | llm
        resp = chain.invoke({
            "note_count": req.note_count,
            "plan_count": req.plan_count,
            "today_minutes": req.today_minutes,
            "concept_count": req.concept_count,
            "review_count": req.review_count,
            "upcoming_reviews": req.upcoming_reviews or "无",
            "recent_topics": req.recent_topics or "无",
            "categories": req.categories or "无",
        })
        import json
        json_str = resp.content.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[1].rsplit("\n", 1)[0]
        return json.loads(json_str)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
