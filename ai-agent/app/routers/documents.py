import json
import re
from io import BytesIO
from pathlib import Path

from docx import Document
from fastapi import APIRouter, File, HTTPException, UploadFile
from langchain_core.prompts import ChatPromptTemplate
from pypdf import PdfReader

from app.services.llm_service import llm

router = APIRouter()
MAX_TEXT_LENGTH = 45000
SUPPORTED = {"pdf", "docx", "txt", "md", "markdown"}

ANALYZE_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是个人知识库的资料整理助手。根据给定学习资料，输出且只输出 JSON 对象，不能输出 Markdown 代码块。
JSON 格式：
{{"title":"简洁标题","summary":"100字以内摘要","suggested_tags":["标签1","标签2"],"difficulty_level":"beginner|intermediate|advanced"}}
标题使用中文；标签 3-5 个；资料不足时也要给出合理结果。"""),
    ("human", "文件名：{filename}\n\n资料正文：\n{content}"),
])


def _extension(filename: str) -> str:
    return Path(filename).suffix.lower().lstrip(".")


def _read_text(filename: str, data: bytes) -> tuple[str, int]:
    extension = _extension(filename)
    if extension not in SUPPORTED:
        raise ValueError("仅支持 PDF、DOCX、TXT 和 Markdown 文档")
    if extension == "pdf":
        reader = PdfReader(BytesIO(data))
        text = "\n\n".join((page.extract_text() or "") for page in reader.pages)
        return text, len(reader.pages)
    if extension == "docx":
        document = Document(BytesIO(data))
        return "\n\n".join(p.text for p in document.paragraphs if p.text.strip()), 0
    for encoding in ("utf-8", "utf-8-sig", "gb18030"):
        try:
            return data.decode(encoding), 0
        except UnicodeDecodeError:
            continue
    raise ValueError("文本编码无法识别，请保存为 UTF-8 后重试")


def _as_markdown(text: str) -> str:
    blocks = [re.sub(r"[ \t]+", " ", item).strip() for item in re.split(r"\n\s*\n", text) if item.strip()]
    return "\n\n".join(blocks)


@router.post("/documents/parse")
async def parse_document(file: UploadFile = File(...)):
    filename = file.filename or "untitled.txt"
    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="上传文件为空")
    if len(data) > 15 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="单个文件不能超过 15MB")
    try:
        raw_text, page_count = _read_text(filename, data)
        content_md = _as_markdown(raw_text)
        if len(content_md) < 20:
            raise ValueError("没有提取到可用文本；扫描版 PDF 请先进行 OCR")
        response = (ANALYZE_PROMPT | llm).invoke({"filename": filename, "content": content_md[:MAX_TEXT_LENGTH]})
        result_text = re.sub(r"^```(?:json)?\s*|\s*```$", "", str(response.content).strip())
        try:
            analysis = json.loads(result_text)
        except json.JSONDecodeError:
            analysis = {}
        fallback_title = Path(filename).stem.replace("_", " ").replace("-", " ") or "导入资料"
        return {
            "title": str(analysis.get("title") or fallback_title)[:200],
            "summary": str(analysis.get("summary") or content_md[:180]),
            "suggested_tags": analysis.get("suggested_tags") if isinstance(analysis.get("suggested_tags"), list) else [],
            "difficulty_level": analysis.get("difficulty_level") if analysis.get("difficulty_level") in {"beginner", "intermediate", "advanced"} else "intermediate",
            "content_md": content_md,
            "source_file": filename,
            "page_count": page_count,
            "char_count": len(content_md),
        }
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=422, detail=f"文档解析失败：{exc}") from exc
