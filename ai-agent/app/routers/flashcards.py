import json
import re

from fastapi import APIRouter, HTTPException
from langchain_core.prompts import ChatPromptTemplate

from app.models.schemas import FlashcardGenerateRequest
from app.services.llm_service import llm

router = APIRouter()

PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是学习卡片设计专家。根据笔记生成高质量复习卡片。
只输出 JSON 数组，不要 Markdown 代码块。每项格式：
{{"question":"题目","answer":"答案","card_type":"qa|choice|judge","options":["选项A","选项B"]}}。
题目应覆盖核心概念、因果关系和易错点；答案简洁准确。选择题 options 必须包含正确答案，问答题 options 为空数组。"""),
    ("human", "标题：{title}\n数量：{count}\n笔记：\n{content}"),
])


@router.post("/generate-flashcards")
def generate_flashcards(req: FlashcardGenerateRequest):
    if not req.content.strip():
        raise HTTPException(status_code=400, detail="笔记内容不能为空")
    count = max(1, min(req.count, 12))
    response = (PROMPT | llm).invoke({
        "title": req.title,
        "count": count,
        "content": req.content[:12000],
    })
    text = response.content.strip()
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text)
    try:
        cards = json.loads(text)
        if not isinstance(cards, list):
            raise ValueError("not an array")
        return {"cards": cards[:count]}
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"AI 返回格式无效: {exc}")
