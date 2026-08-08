import os
from dotenv import load_dotenv

load_dotenv()

# HuggingFace 镜像（必须在导入模型前设置）
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "your-api-key")
DEEPSEEK_BASE_URL = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")

CHROMA_PERSIST_DIR = os.getenv("CHROMA_PERSIST_DIR", "./chroma_data")
CHROMA_COLLECTION = "knowledge_notes"

SPRING_BOOT_URL = os.getenv("SPRING_BOOT_URL", "http://localhost:8080")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
