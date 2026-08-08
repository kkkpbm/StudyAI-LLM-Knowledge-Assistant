package com.ka.service;

import java.util.List;
import java.util.Map;

public interface LearningCenterService {
    List<Map<String, Object>> generateFlashcards(Long userId, Long noteId, int count);
    List<Map<String, Object>> getDueFlashcards(Long userId, int limit);
    Map<String, Object> reviewFlashcard(Long userId, Long cardId, int quality);
    List<Map<String, Object>> semanticSearch(Long userId, String query, int topK);
    Map<String, Object> weeklyReport(Long userId);
    List<Map<String, Object>> achievements(Long userId);
    List<Map<String, Object>> trash(Long userId);
    Long restoreTrash(Long userId, Long trashId);
    void permanentlyDeleteTrash(Long userId, Long trashId);
    List<Map<String, Object>> versions(Long userId, Long noteId);
    void restoreVersion(Long userId, Long noteId, Long versionId);
    List<Long> importNotes(Long userId, List<Map<String, Object>> notes);
    List<Map<String, Object>> exportNotes(Long userId);
}
