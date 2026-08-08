from langchain_openai import ChatOpenAI
from app.config import DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL, DEEPSEEK_MODEL

# 普通 LLM（非流式，用于摘要等场景）
llm = ChatOpenAI(
    model=DEEPSEEK_MODEL,
    api_key=DEEPSEEK_API_KEY,
    base_url=DEEPSEEK_BASE_URL,
    temperature=0.7,
)

# 流式 LLM（用于聊天，逐 token 返回）
streaming_llm = ChatOpenAI(
    model=DEEPSEEK_MODEL,
    api_key=DEEPSEEK_API_KEY,
    base_url=DEEPSEEK_BASE_URL,
    temperature=0.7,
    streaming=True,
)
