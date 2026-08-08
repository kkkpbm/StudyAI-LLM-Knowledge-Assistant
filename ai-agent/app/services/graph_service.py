import json
from langchain_core.prompts import ChatPromptTemplate
from app.services.llm_service import llm

GRAPH_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """Extract knowledge concepts and their relationships from the text.
Output ONLY a valid JSON array of relations, no other text.

Format:
- Array of objects with source, target, type, and weight fields
- source/target: short concept names (max 20 chars)
- type: prerequisite (A must be learned before B), related (general association), extends (B builds on A), contradicts
- weight: 0.0-1.0 representing relationship strength
- Extract 3-10 relations. If none found, return []."""),
    ("human", "{content}"),
])


def extract_knowledge_graph(content: str) -> list[dict]:
    """Extract knowledge graph relations from text content."""
    chain = GRAPH_PROMPT | llm
    response = chain.invoke({"content": content[:4000]})
    try:
        json_str = response.content.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[1].rsplit("\n", 1)[0]
        return json.loads(json_str)
    except (json.JSONDecodeError, ValueError):
        return []


def extract_concepts(content: str) -> list[str]:
    """Extract key concepts from text."""
    relations = extract_knowledge_graph(content)
    concepts = set()
    for r in relations:
        concepts.add(r["source"])
        concepts.add(r["target"])
    return list(concepts)
