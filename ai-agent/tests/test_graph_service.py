"""Tests for graph service."""
import pytest
from unittest.mock import patch, MagicMock
from app.services.graph_service import extract_knowledge_graph, extract_concepts


class TestExtractKnowledgeGraph:
    """Test knowledge graph extraction."""

    def test_extract_valid_json(self):
        """Test extraction with valid JSON response."""
        with patch("app.services.graph_service.GRAPH_PROMPT") as mock_prompt:
            mock_response = MagicMock()
            mock_response.content = '''[
                {"source": "Python", "target": "Programming", "type": "related", "weight": 0.9},
                {"source": "Machine Learning", "target": "AI", "type": "extends", "weight": 0.8}
            ]'''
            
            chain_mock = MagicMock()
            chain_mock.invoke.return_value = mock_response
            mock_prompt.__or__ = MagicMock(return_value=chain_mock)
            
            result = extract_knowledge_graph("Some text about Python and ML")
            
            assert len(result) == 2
            assert result[0]["source"] == "Python"
            assert result[1]["type"] == "extends"

    def test_extract_with_markdown_block(self):
        """Test extraction with markdown code block."""
        with patch("app.services.graph_service.GRAPH_PROMPT") as mock_prompt:
            mock_response = MagicMock()
            mock_response.content = '''```json
[
    {"source": "A", "target": "B", "type": "prerequisite", "weight": 0.7}
]
```'''
            
            chain_mock = MagicMock()
            chain_mock.invoke.return_value = mock_response
            mock_prompt.__or__ = MagicMock(return_value=chain_mock)
            
            result = extract_knowledge_graph("Text")
            
            assert len(result) == 1
            assert result[0]["source"] == "A"

    def test_extract_invalid_json_returns_empty(self):
        """Test that invalid JSON returns empty list."""
        with patch("app.services.graph_service.GRAPH_PROMPT") as mock_prompt:
            mock_response = MagicMock()
            mock_response.content = "This is not valid JSON"
            
            chain_mock = MagicMock()
            chain_mock.invoke.return_value = mock_response
            mock_prompt.__or__ = MagicMock(return_value=chain_mock)
            
            result = extract_knowledge_graph("Text")
            
            assert result == []


class TestExtractConcepts:
    """Test concept extraction."""

    def test_extract_concepts(self):
        """Test extracting unique concepts from relations."""
        with patch("app.services.graph_service.extract_knowledge_graph") as mock_extract:
            mock_extract.return_value = [
                {"source": "Python", "target": "Programming", "type": "related", "weight": 0.9},
                {"source": "Python", "target": "Data Science", "type": "related", "weight": 0.8},
                {"source": "ML", "target": "AI", "type": "extends", "weight": 0.7}
            ]
            
            concepts = extract_concepts("Some text")
            
            assert len(concepts) == 5
            assert "Python" in concepts
            assert "Programming" in concepts
            assert "Data Science" in concepts
            assert "ML" in concepts
            assert "AI" in concepts

    def test_extract_concepts_empty(self):
        """Test extracting concepts from empty relations."""
        with patch("app.services.graph_service.extract_knowledge_graph") as mock_extract:
            mock_extract.return_value = []
            
            concepts = extract_concepts("Text")
            
            assert concepts == []