package com.ka.service;

import com.ka.entity.KnowledgeRelation;

import java.util.List;
import java.util.Map;

public interface KnowledgeRelationService {
    List<KnowledgeRelation> getAllRelations(Long userId);

    List<KnowledgeRelation> getRelationsByNoteId(Long userId, Long noteId);

    long getConceptCount(Long userId);

    List<KnowledgeRelation> buildGraphForNote(Long noteId, String content);

    Map<String, Object> buildGraphForAllNotes(Long userId);

    void deleteAllRelations(Long userId);

    void deleteRelationsByNoteId(Long userId, Long noteId);

    Map<String, Object> getConceptDetail(Long userId, String conceptName);

    KnowledgeRelation createRelation(Long userId, Long noteId, String source, String target,
                                     String relationType, Double weight);

    KnowledgeRelation updateRelation(Long userId, Long relationId, String source, String target,
                                     String relationType, Double weight);

    void deleteRelation(Long userId, Long relationId);

    List<Map<String, Object>> recommendRelations(Long userId);
}
