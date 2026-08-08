package com.ka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.BusinessException;
import com.ka.entity.KnowledgeRelation;
import com.ka.entity.Note;
import com.ka.mapper.KnowledgeRelationMapper;
import com.ka.mapper.NoteMapper;
import com.ka.service.AiAgentClient;
import com.ka.service.KnowledgeRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRelationServiceImpl implements KnowledgeRelationService {

    private final KnowledgeRelationMapper relationMapper;
    private final NoteMapper noteMapper;
    private final AiAgentClient aiAgentClient;

    private Note requireOwnedNote(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) {
            throw new BusinessException(404, "Note not found");
        }
        return note;
    }

    /**
     * 获取用户所有知识关系
     */
    public List<KnowledgeRelation> getAllRelations(Long userId) {
        List<Note> userNotes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));
        if (userNotes.isEmpty()) return List.of();

        List<Long> noteIds = userNotes.stream().map(Note::getId).toList();
        return relationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeRelation>().in(KnowledgeRelation::getNoteId, noteIds));
    }

    /**
     * 获取指定笔记的知识关系
     */
    public List<KnowledgeRelation> getRelationsByNoteId(Long userId, Long noteId) {
        requireOwnedNote(userId, noteId);
        return relationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeRelation>().eq(KnowledgeRelation::getNoteId, noteId));
    }

    /**
     * 获取用户知识图谱中的概念数量（去重）
     */
    public long getConceptCount(Long userId) {
        List<KnowledgeRelation> all = getAllRelations(userId);
        Set<String> concepts = new HashSet<>();
        for (KnowledgeRelation r : all) {
            concepts.add(r.getSource());
            concepts.add(r.getTarget());
        }
        return concepts.size();
    }

    /**
     * 为指定笔记构建知识图谱（调用 AI 提取关系并保存）
     */
    @Transactional
    public List<KnowledgeRelation> buildGraphForNote(Long noteId, String content) {
        // 删除旧关系
        relationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getNoteId, noteId));

        if (content == null || content.isBlank()) return List.of();

        try {
            Object result = aiAgentClient.extractGraph(content);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> relations = extractRelationsList(result);
            if (relations == null || relations.isEmpty()) return List.of();

            List<KnowledgeRelation> saved = new ArrayList<>();
            for (Map<String, Object> rel : relations) {
                KnowledgeRelation kr = new KnowledgeRelation();
                kr.setNoteId(noteId);
                kr.setSource(Objects.toString(rel.get("source"), ""));
                kr.setTarget(Objects.toString(rel.get("target"), ""));
                kr.setRelationType(Objects.toString(rel.get("type"), "related"));
                Object weightObj = rel.get("weight");
                kr.setWeight(weightObj instanceof Number ? ((Number) weightObj).doubleValue() : 0.5);
                relationMapper.insert(kr);
                saved.add(kr);
            }
            return saved;
        } catch (Exception e) {
            log.warn("Failed to extract graph for note {}: {}", noteId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 为所有用户笔记批量构建知识图谱
     */
    @Transactional
    public Map<String, Object> buildGraphForAllNotes(Long userId) {
        List<Note> notes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));

        // 删除用户所有旧关系
        List<Long> noteIds = notes.stream().map(Note::getId).toList();
        if (!noteIds.isEmpty()) {
            relationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                    .in(KnowledgeRelation::getNoteId, noteIds));
        }

        int totalRelations = 0;
        int processedNotes = 0;

        for (Note note : notes) {
            if (note.getContentMd() == null || note.getContentMd().isBlank()) continue;
            try {
                List<KnowledgeRelation> rels = buildGraphForNote(note.getId(), note.getContentMd());
                totalRelations += rels.size();
                processedNotes++;
            } catch (Exception e) {
                log.warn("Failed to build graph for note {}: {}", note.getId(), e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processedNotes", processedNotes);
        result.put("totalRelations", totalRelations);
        result.put("totalNotes", notes.size());
        return result;
    }

    /**
     * 删除用户所有知识关系
     */
    @Transactional
    public void deleteAllRelations(Long userId) {
        List<Note> notes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));
        List<Long> noteIds = notes.stream().map(Note::getId).toList();
        if (!noteIds.isEmpty()) {
            relationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                    .in(KnowledgeRelation::getNoteId, noteIds));
        }
    }

    /**
     * 删除指定笔记的知识关系
     */
    @Transactional
    public void deleteRelationsByNoteId(Long userId, Long noteId) {
        requireOwnedNote(userId, noteId);
        relationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getNoteId, noteId));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRelationsList(Object aiResult) {
        if (aiResult instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) aiResult;
            Object relations = map.get("relations");
            if (relations instanceof List) {
                return (List<Map<String, Object>>) relations;
            }
        }
        return List.of();
    }

    /**
     * 获取某个概念的详情：关联关系、连接概念、相关笔记
     */
    public Map<String, Object> getConceptDetail(Long userId, String conceptName) {
        // 获取该用户所有关系
        List<KnowledgeRelation> allRelations = getAllRelations(userId);

        // 筛选涉及该概念的关系
        List<KnowledgeRelation> matchedRelations = allRelations.stream()
                .filter(r -> conceptName.equals(r.getSource()) || conceptName.equals(r.getTarget()))
                .toList();

        // 收集连接的概念
        Set<String> connectedConcepts = new LinkedHashSet<>();
        for (KnowledgeRelation r : matchedRelations) {
            if (conceptName.equals(r.getSource())) {
                connectedConcepts.add(r.getTarget());
            } else {
                connectedConcepts.add(r.getSource());
            }
        }

        // 收集相关的笔记
        Set<Long> noteIds = matchedRelations.stream()
                .map(KnowledgeRelation::getNoteId)
                .collect(java.util.stream.Collectors.toSet());
        List<Note> relatedNotes = noteIds.isEmpty() ? List.of()
                : noteMapper.selectBatchIds(noteIds).stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();

        // 构建笔记简要信息
        List<Map<String, Object>> noteInfos = relatedNotes.stream().map(n -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", n.getId());
            info.put("title", n.getTitle());
            info.put("difficultyLevel", n.getDifficultyLevel());
            return info;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conceptName", conceptName);
        result.put("relationCount", matchedRelations.size());
        result.put("relations", matchedRelations);
        result.put("connectedConcepts", connectedConcepts);
        result.put("relatedNotes", noteInfos);
        return result;
    }

    @Transactional
    public KnowledgeRelation createRelation(Long userId, Long noteId, String source, String target,
                                            String relationType, Double weight) {
        requireOwnedNote(userId, noteId);
        if (source == null || source.isBlank() || target == null || target.isBlank()) {
            throw new BusinessException(400, "Source and target are required");
        }
        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setNoteId(noteId);
        relation.setSource(source.trim());
        relation.setTarget(target.trim());
        relation.setRelationType(relationType == null ? "related" : relationType);
        relation.setWeight(weight == null ? 0.5 : Math.max(0.0, Math.min(1.0, weight)));
        relationMapper.insert(relation);
        return relation;
    }

    @Transactional
    public KnowledgeRelation updateRelation(Long userId, Long relationId, String source, String target,
                                            String relationType, Double weight) {
        KnowledgeRelation relation = relationMapper.selectById(relationId);
        if (relation == null) throw new BusinessException(404, "Relation not found");
        requireOwnedNote(userId, relation.getNoteId());
        if (source != null && !source.isBlank()) relation.setSource(source.trim());
        if (target != null && !target.isBlank()) relation.setTarget(target.trim());
        if (relationType != null) relation.setRelationType(relationType);
        if (weight != null) relation.setWeight(Math.max(0.0, Math.min(1.0, weight)));
        relationMapper.updateById(relation);
        return relation;
    }

    @Transactional
    public void deleteRelation(Long userId, Long relationId) {
        KnowledgeRelation relation = relationMapper.selectById(relationId);
        if (relation == null) throw new BusinessException(404, "Relation not found");
        requireOwnedNote(userId, relation.getNoteId());
        relationMapper.deleteById(relationId);
    }

    public List<Map<String, Object>> recommendRelations(Long userId) {
        List<KnowledgeRelation> relations = getAllRelations(userId);
        Map<Long, LinkedHashSet<String>> conceptsByNote = new LinkedHashMap<>();
        Set<String> existing = new HashSet<>();
        for (KnowledgeRelation relation : relations) {
            LinkedHashSet<String> concepts = conceptsByNote.computeIfAbsent(
                    relation.getNoteId(), ignored -> new LinkedHashSet<>());
            concepts.add(relation.getSource());
            concepts.add(relation.getTarget());
            existing.add(relation.getSource() + "\u0000" + relation.getTarget());
            existing.add(relation.getTarget() + "\u0000" + relation.getSource());
        }
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Map.Entry<Long, LinkedHashSet<String>> entry : conceptsByNote.entrySet()) {
            List<String> concepts = new ArrayList<>(entry.getValue());
            for (int i = 0; i < concepts.size() && recommendations.size() < 12; i++) {
                for (int j = i + 1; j < concepts.size() && recommendations.size() < 12; j++) {
                    if (!existing.contains(concepts.get(i) + "\u0000" + concepts.get(j))) {
                        recommendations.add(Map.of(
                                "noteId", entry.getKey(), "source", concepts.get(i),
                                "target", concepts.get(j), "relationType", "related",
                                "weight", 0.4,
                                "reason", "两个概念出现在同一篇笔记中，但尚未建立直接关系"));
                    }
                }
            }
        }
        return recommendations;
    }
}
