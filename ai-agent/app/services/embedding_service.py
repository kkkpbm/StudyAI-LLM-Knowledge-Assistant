from app.services.vector_store import get_collection


def sync_note_embedding(note_id: int, user_id: int, content: str, title: str = "") -> None:
    """Store note in ChromaDB with built-in embedding."""
    collection = get_collection()
    doc_text = f"Title: {title}\n\n{content}" if title else content

    collection.upsert(
        ids=[str(note_id)],
        documents=[doc_text],
        metadatas=[{
            "user_id": str(user_id),
            "note_id": str(note_id),
            "title": title,
        }],
    )


def delete_note_embedding(note_id: int) -> None:
    collection = get_collection()
    collection.delete(ids=[str(note_id)])


def search_by_vector(query: str, user_id: int, top_k: int = 5):
    """Semantic search using ChromaDB built-in embedding."""
    collection = get_collection()
    results = collection.query(
        query_texts=[query],
        n_results=top_k,
        where={"user_id": str(user_id)},
        include=["documents", "metadatas", "distances"],
    )
    ids = results.get("ids", [[]])[0]
    docs = results.get("documents", [[]])[0]
    metas = results.get("metadatas", [[]])[0]
    distances = results.get("distances", [[]])[0]

    items = []
    for i in range(len(ids)):
        items.append({
            "note_id": ids[i],
            "title": (metas[i] or {}).get("title", "") if i < len(metas) else "",
            "content": docs[i][:500] if docs[i] else "",
            "score": float(1 - (distances[i] if distances else 0)),
        })
    return items
