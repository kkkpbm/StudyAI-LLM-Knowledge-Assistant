"""API endpoint integration tests."""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from app.main import app


@pytest.fixture
def client():
    """Create test client."""
    return TestClient(app)


class TestHealthEndpoint:
    """Test health check endpoint."""

    def test_health_returns_ok(self, client):
        """Test health endpoint returns ok status."""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {"status": "ok"}


class TestChatEndpoint:
    """Test chat endpoint."""

    @patch("app.routers.chat.rag_chat")
    def test_chat_success(self, mock_rag_chat, client):
        """Test successful chat request."""
        mock_rag_chat.return_value = "This is a test answer."
        
        response = client.post(
            "/agent/chat",
            json={
                "user_id": 1,
                "question": "What is AI?"
            }
        )
        
        assert response.status_code == 200
        assert "answer" in response.json()
        assert response.json()["answer"] == "This is a test answer."

    @patch("app.routers.chat.rag_chat")
    def test_chat_with_note_id(self, mock_rag_chat, client):
        """Test chat with specific note ID."""
        mock_rag_chat.return_value = "Answer based on specific note."
        
        response = client.post(
            "/agent/chat",
            json={
                "user_id": 1,
                "note_id": 123,
                "question": "What is in this note?"
            }
        )
        
        assert response.status_code == 200
        mock_rag_chat.assert_called_once_with(1, "What is in this note?", 123)

    @patch("app.routers.chat.rag_chat")
    def test_chat_handles_error(self, mock_rag_chat, client):
        """Test chat handles errors gracefully."""
        mock_rag_chat.side_effect = Exception("LLM service error")
        
        response = client.post(
            "/agent/chat",
            json={
                "user_id": 1,
                "question": "Test question"
            }
        )
        
        assert response.status_code == 500


class TestSummarizeEndpoint:
    """Test summarize endpoint."""

    @patch("app.routers.summarize.llm")
    def test_summarize_success(self, mock_llm, client):
        """Test successful summarization."""
        mock_response = MagicMock()
        mock_response.content = "• Point 1\n• Point 2\n• Point 3"
        mock_llm.__or__ = MagicMock(return_value=MagicMock(invoke=MagicMock(return_value=mock_response)))
        
        response = client.post(
            "/agent/summarize",
            json={
                "content": "Long text to summarize..."
            }
        )
        
        assert response.status_code == 200
        assert "summary" in response.json()


class TestReviewEndpoint:
    """Test review endpoint."""

    def test_next_review_success(self, client):
        """Test next review calculation."""
        response = client.post(
            "/agent/next-review",
            json={
                "user_id": 1,
                "note_id": 123,
                "quality": 4,
                "interval_days": 6,
                "ease_factor": 2.5
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert "next_interval_days" in data
        assert "ease_factor" in data
        assert data["next_interval_days"] > 0


class TestEmbeddingsEndpoint:
    """Test embeddings endpoints."""

    @patch("app.routers.embeddings.sync_note_embedding")
    def test_sync_embedding_success(self, mock_sync, client):
        """Test successful embedding sync."""
        response = client.post(
            "/agent/embeddings/sync",
            json={
                "note_id": 123,
                "user_id": 1,
                "content": "Note content",
                "title": "Note Title"
            }
        )
        
        assert response.status_code == 200
        assert response.json()["status"] == "ok"

    @patch("app.routers.embeddings.delete_note_embedding")
    def test_delete_embedding_success(self, mock_delete, client):
        """Test successful embedding deletion."""
        response = client.delete("/agent/embeddings/123")
        
        assert response.status_code == 200
        assert response.json()["status"] == "ok"

    @patch("app.routers.embeddings.search_by_vector")
    def test_search_notes_success(self, mock_search, client):
        """Test successful note search."""
        mock_search.return_value = [
            {"note_id": "1", "content": "Result 1", "score": 0.9}
        ]
        
        response = client.post(
            "/agent/search-notes",
            json={
                "user_id": 1,
                "query": "machine learning",
                "top_k": 5
            }
        )
        
        assert response.status_code == 200
        assert "results" in response.json()