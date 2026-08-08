import threading
import chromadb
from chromadb.config import Settings
from chromadb.utils import embedding_functions
from app.config import CHROMA_PERSIST_DIR, CHROMA_COLLECTION, EMBEDDING_MODEL

_lock = threading.Lock()
_chroma_client = None
_embedding_function = None


def get_embedding_function():
    global _embedding_function
    if _embedding_function is None:
        _embedding_function = embedding_functions.SentenceTransformerEmbeddingFunction(
            model_name=EMBEDDING_MODEL
        )
    return _embedding_function


def get_chroma():
    global _chroma_client
    if _chroma_client is None:
        with _lock:
            if _chroma_client is None:
                _chroma_client = chromadb.PersistentClient(
                    path=CHROMA_PERSIST_DIR,
                    settings=Settings(anonymized_telemetry=False),
                )
    return _chroma_client


def get_collection():
    client = get_chroma()
    ef = get_embedding_function()

    with _lock:
        try:
            existing = client.get_collection(name=CHROMA_COLLECTION)
            ef_config = existing._embedding_function
            if ef_config is not None:
                ef_name = getattr(ef_config, 'name', None) or ef_config.__class__.__name__
                if ef_name != ef.name():
                    client.delete_collection(name=CHROMA_COLLECTION)
                    return client.create_collection(
                        name=CHROMA_COLLECTION,
                        embedding_function=ef,
                        metadata={"hnsw:space": "cosine"},
                    )
            return existing
        except Exception:
            return client.create_collection(
                name=CHROMA_COLLECTION,
                embedding_function=ef,
                metadata={"hnsw:space": "cosine"},
            )
