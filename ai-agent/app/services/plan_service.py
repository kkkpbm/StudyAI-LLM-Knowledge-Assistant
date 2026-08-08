import json
from langchain_core.prompts import ChatPromptTemplate
from app.services.llm_service import llm

PLAN_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """You are a learning path planner. Create a structured study plan based on the user's goal.
Output ONLY a valid JSON, no other text.

Format:
- title: Plan title (string)
- description: Brief overview (string)
- phases: Array of phases, each with name, description, topics array, estimated_days, resources_suggestion"""),
    ("human", "My learning goal: {goal}"),
])


def generate_learning_plan(goal: str) -> dict:
    chain = PLAN_PROMPT | llm
    response = chain.invoke({"goal": goal})
    try:
        json_str = response.content.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[1].rsplit("\n", 1)[0]
        return json.loads(json_str)
    except (json.JSONDecodeError, ValueError):
        return {
            "title": "Custom Learning Plan",
            "description": f"Plan for: {goal}",
            "phases": [
                {"name": "Getting Started", "description": goal, "topics": [goal], "estimated_days": 14}
            ],
        }
