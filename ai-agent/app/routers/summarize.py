from fastapi import APIRouter, HTTPException
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel
from app.models.schemas import SummarizeRequest, AssessRequest
from app.services.llm_service import llm

router = APIRouter()

SUMMARIZE_PROMPT = ChatPromptTemplate.from_messages([
    ("system", "Summarize the following text concisely in 3-5 bullet points in Chinese. Be accurate."),
    ("human", "{content}"),
])

ASSESS_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """Assess the difficulty of the following learning material.
Reply with ONLY a valid JSON object, no other text.

Format:
- level: one of beginner, intermediate, advanced
- reason: brief explanation in Chinese"""),
    ("human", "{content}"),
])


@router.post("/summarize")
def summarize(req: SummarizeRequest):
    try:
        chain = SUMMARIZE_PROMPT | llm
        resp = chain.invoke({"content": req.content[:4000]})
        return {"summary": resp.content}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/assess-difficulty")
def assess_difficulty(req: AssessRequest):
    try:
        chain = ASSESS_PROMPT | llm
        resp = chain.invoke({"content": req.content[:4000]})
        import json
        json_str = resp.content.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[1].rsplit("\n", 1)[0]
        result = json.loads(json_str)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


TAG_SUGGEST_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """Analyze the following content and suggest the best category and 2-4 tags for organizing it as a study note.
Reply with ONLY a valid JSON object, no other text.

Format:
- category: a suitable category name (e.g. "前端", "后端", "AI/机器学习", "基础理论", "工具/效率")
- tags: array of 2-4 tag strings
- reason: brief explanation in Chinese why you picked these"""),
    ("human", "{content}"),
])


class SuggestTagsRequest(BaseModel):
    content: str


@router.post("/suggest-tags")
def suggest_tags(req: SuggestTagsRequest):
    """根据笔记内容推荐分类和标签"""
    try:
        chain = TAG_SUGGEST_PROMPT | llm
        resp = chain.invoke({"content": req.content[:3000]})
        import json
        json_str = resp.content.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[1].rsplit("\n", 1)[0]
        return json.loads(json_str)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
