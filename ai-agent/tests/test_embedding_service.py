"""Tests for embedding service."""
import pytest
from unittest.mock import patch, MagicMock
from app.services.embedding_service import sync_note_embedding, delete_note_embedding, search_by_vector


class TestSyncNoteEmbedding:
    """Test note embedding sync."""

    @patch("app.services.embedding_service.get_collection")
    def test_sync_note_with_title(self, mock_get_collection):
        """Test syncing note with title."""
        mock_collection = MagicMock()
        mock_get_collection.return_value = mock_collection
        
        sync_note_embedding(
            note_id=123,
            user_id=1,
            content="Note content",
            title="My Title"
        )
        
        mock_collection.upsert.assert_called_once()
        call_args = mock_collection.upsert.call_args
        assert call_args[1]["ids"] == ["123"]
        assert "My Title" in call_args[1]["documents"][0]
        assert "Note content" in call_args[1]["documents"][0]

    @patch("app.services.embedding_service.get_collection")
    def test_sync_note_without_title(self, mock_get_collection):
        """Test syncing note without title."""
        mock_collection = MagicMock()
        mock_get_collection.return_value = mock_collection
        
        sync_note_embedding(
            note_id=456,
            user_id=2,
            content="Just content",
            title=""
        )
        
        mock_collection.upsert.assert_called_once()
        call_args = mock_collection.upsert.call_args
        assert call_args[1]["documents"][0] == "Just content"


class TestDeleteNoteEmbedding:
    """Test note embedding deletion."""

    @patch("app.services.embedding_service.get_collection")
    def test_delete_embedding(self, mock_get_collection):
        """Test deleting note embedding."""
        mock_collection = MagicMock()
        mock_get_collection.return_value = mock_collection
        
        delete_note_embedding(123)
        
        mock_collection.delete.assert_called_once_with(ids=["123"])


class TestSearchByVector:
    """Test vector search."""

    @patch("app.services.embedding_service.get_collection")
    def test_search_returns_results(self, mock_get_collection):
        """Test search returns formatted results."""
        mock_collection = MagicMock()
        mock_collection.query.return_value = {
            "ids": [["1", "2"]],
            "documents": [["Content 1", "Content 2"]],
            "metadatas": [[{"user_id": "1", "note_id": "1"}, {"user_id": "1", "note_id": "2"}]],
            "distances": [[0.1, 0.2]]
        }
        mock_get_collection.return_value = mock_collection
        
        results = search_by_vector("test query", user_id=1, top_k=2)
        
        assert len(results) == 2
        assert results[0]["note_id"] == "1"
        assert results[0]["score"] == 0.9  # 1 - 0.1
        assert results[1]["score"] == 0.8  # 1 - 0.2

    @patch("app.services.embedding_service.get_collection")
    def test_search_empty_results(self, mock_get_collection):
        """Test search with no results."""
        mock_collection = MagicMock()
        mock_collection.query.return_value = {
            "ids": [[]],
            "documents": [[]],
            "metadatas": [[]],
            "distances": [[]]
        }
        mock_get_collection.return_value = mock_collection
        
        results = search_by_vector("nonexistent", user_id=1)
        
        assert results == []