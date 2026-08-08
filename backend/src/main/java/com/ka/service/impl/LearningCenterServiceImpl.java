package com.ka.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ka.common.BusinessException;
import com.ka.entity.Note;
import com.ka.mapper.NoteMapper;
import com.ka.service.AiAgentClient;
import com.ka.service.LearningCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearningCenterServiceImpl implements LearningCenterService {
    private final JdbcTemplate jdbc;
    private final NoteMapper noteMapper;
    private final AiAgentClient aiAgentClient;
    private final ObjectMapper objectMapper;

    private Note ownedNote(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) {
            throw new BusinessException(404, "Note not found");
        }
        return note;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<Map<String, Object>> generateFlashcards(Long userId, Long noteId, int count) {
        Note note = ownedNote(userId, noteId);
        Object raw = aiAgentClient.generateFlashcards(note.getTitle(), note.getContentMd(), count);
        Object cardsValue = raw instanceof Map<?, ?> map ? map.get("cards") : null;
        List<Map<String, Object>> cards = cardsValue instanceof List<?> list
                ? (List<Map<String, Object>>) list : List.of();
        for (Map<String, Object> card : cards) {
            String options;
            try {
                options = objectMapper.writeValueAsString(card.getOrDefault("options", List.of()));
            } catch (Exception e) {
                options = "[]";
            }
            jdbc.update("""
                    INSERT INTO flashcards
                    (user_id,note_id,question,answer,card_type,options_json,next_review_at)
                    VALUES (?,?,?,?,?,?,CURRENT_DATE)
                    """, userId, noteId, Objects.toString(card.get("question"), ""),
                    Objects.toString(card.get("answer"), ""),
                    Objects.toString(card.get("card_type"), "qa"), options);
        }
        return getDueFlashcards(userId, 100);
    }

    public List<Map<String, Object>> getDueFlashcards(Long userId, int limit) {
        return jdbc.queryForList("""
                SELECT f.*, n.title AS note_title
                FROM flashcards f JOIN notes n ON n.id=f.note_id
                WHERE f.user_id=? AND f.next_review_at<=CURRENT_DATE AND n.deleted=0
                ORDER BY f.next_review_at, f.id LIMIT ?
                """, userId, Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    public Map<String, Object> reviewFlashcard(Long userId, Long cardId, int quality) {
        if (quality < 0 || quality > 5) throw new BusinessException(400, "Quality must be 0-5");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM flashcards WHERE id=? AND user_id=?", cardId, userId);
        if (rows.isEmpty()) throw new BusinessException(404, "Flashcard not found");
        Map<String, Object> card = rows.get(0);
        int interval = ((Number) card.getOrDefault("interval_days", 1)).intValue();
        int reps = ((Number) card.getOrDefault("repetitions", 0)).intValue();
        double ease = ((Number) card.getOrDefault("ease_factor", 2.5)).doubleValue();
        int nextInterval;
        if (quality < 3) {
            reps = 0;
            nextInterval = 1;
        } else {
            nextInterval = reps == 0 ? 1 : reps == 1 ? 6 : Math.max(1, (int) Math.round(interval * ease));
            ease = Math.max(1.3, ease + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)));
            reps++;
        }
        LocalDate nextDate = LocalDate.now().plusDays(nextInterval);
        jdbc.update("""
                UPDATE flashcards SET next_review_at=?,interval_days=?,ease_factor=?,
                repetitions=?,last_quality=? WHERE id=? AND user_id=?
                """, nextDate, nextInterval, ease, reps, quality, cardId, userId);
        jdbc.update("INSERT INTO flashcard_attempts(user_id,flashcard_id,quality,correct) VALUES(?,?,?,?)",
                userId, cardId, quality, quality >= 3 ? 1 : 0);
        return Map.of("nextReviewAt", nextDate, "intervalDays", nextInterval,
                "easeFactor", ease, "repetitions", reps);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> semanticSearch(Long userId, String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        Object raw = aiAgentClient.searchNotes(userId, query, topK);
        Object value = raw instanceof Map<?, ?> map ? map.get("results") : null;
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> results = (List<Map<String, Object>>) list;
        for (Map<String, Object> result : results) {
            Long noteId = Long.valueOf(result.get("note_id").toString());
            Note note = noteMapper.selectById(noteId);
            if (note != null && userId.equals(note.getUserId())) {
                result.put("title", note.getTitle());
                result.put("categoryId", note.getCategoryId());
                result.put("updatedAt", note.getUpdatedAt());
            }
        }
        return results.stream().filter(r -> r.containsKey("title")).toList();
    }

    public Map<String, Object> weeklyReport(Long userId) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("minutes", jdbc.queryForObject("""
                SELECT COALESCE(SUM(duration_minutes),0) FROM learning_records
                WHERE user_id=? AND date>=DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY)
                """, Long.class, userId));
        report.put("newNotes", jdbc.queryForObject("""
                SELECT COUNT(*) FROM notes WHERE user_id=? AND deleted=0
                AND created_at>=DATE_SUB(NOW(), INTERVAL 7 DAY)
                """, Long.class, userId));
        report.put("reviews", jdbc.queryForObject("""
                SELECT COUNT(*) FROM flashcard_attempts WHERE user_id=?
                AND created_at>=DATE_SUB(NOW(), INTERVAL 7 DAY)
                """, Long.class, userId));
        report.put("correctRate", jdbc.queryForObject("""
                SELECT COALESCE(ROUND(AVG(correct)*100),0) FROM flashcard_attempts WHERE user_id=?
                AND created_at>=DATE_SUB(NOW(), INTERVAL 7 DAY)
                """, Integer.class, userId));
        report.put("daily", jdbc.queryForList("""
                SELECT date, SUM(duration_minutes) minutes FROM learning_records
                WHERE user_id=? AND date>=DATE_SUB(CURRENT_DATE, INTERVAL 6 DAY)
                GROUP BY date ORDER BY date
                """, userId));
        report.put("categoryDistribution", jdbc.queryForList("""
                SELECT COALESCE(c.name,'未分类') name, COUNT(*) value
                FROM notes n LEFT JOIN categories c ON c.id=n.category_id
                WHERE n.user_id=? AND n.deleted=0 GROUP BY c.id,c.name ORDER BY value DESC
                """, userId));
        return report;
    }

    public List<Map<String, Object>> achievements(Long userId) {
        long notes = jdbc.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id=? AND deleted=0", Long.class, userId);
        long minutes = jdbc.queryForObject("SELECT COALESCE(SUM(duration_minutes),0) FROM learning_records WHERE user_id=?", Long.class, userId);
        long reviews = jdbc.queryForObject("SELECT COUNT(*) FROM flashcard_attempts WHERE user_id=?", Long.class, userId);
        long concepts = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT concept) FROM (
                  SELECT kr.source concept FROM knowledge_relations kr JOIN notes n ON n.id=kr.note_id WHERE n.user_id=? AND n.deleted=0
                  UNION SELECT kr.target FROM knowledge_relations kr JOIN notes n ON n.id=kr.note_id WHERE n.user_id=? AND n.deleted=0
                ) x
                """, Long.class, userId, userId);
        return List.of(
                achievement("first-note", "知识启程", "创建第一篇笔记", notes, 1),
                achievement("note-keeper", "笔记收藏家", "累计创建 20 篇笔记", notes, 20),
                achievement("focused", "专注十小时", "累计学习 600 分钟", minutes, 600),
                achievement("reviewer", "记忆训练师", "完成 50 次卡片复习", reviews, 50),
                achievement("connector", "知识连接者", "图谱拥有 30 个概念", concepts, 30)
        );
    }

    private Map<String, Object> achievement(String code, String name, String description, long value, long target) {
        return Map.of("code", code, "name", name, "description", description,
                "value", value, "target", target, "unlocked", value >= target,
                "progress", Math.min(100, Math.round(value * 100.0 / target)));
    }

    public List<Map<String, Object>> trash(Long userId) {
        return jdbc.queryForList("SELECT * FROM note_trash WHERE user_id=? ORDER BY deleted_at DESC", userId);
    }

    @Transactional
    public Long restoreTrash(Long userId, Long trashId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM note_trash WHERE id=? AND user_id=?", trashId, userId);
        if (rows.isEmpty()) throw new BusinessException(404, "Trash item not found");
        Map<String, Object> row = rows.get(0);
        jdbc.update("""
                INSERT INTO notes(user_id,title,content_md,category_id,difficulty_level,deleted)
                VALUES(?,?,?,?,?,0)
                """, userId, row.get("title"), row.get("content_md"), row.get("category_id"), row.get("difficulty_level"));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("DELETE FROM note_trash WHERE id=? AND user_id=?", trashId, userId);
        aiAgentClient.syncEmbeddingAsync(id, userId, Objects.toString(row.get("content_md"), ""),
                Objects.toString(row.get("title"), ""));
        return id;
    }

    public void permanentlyDeleteTrash(Long userId, Long trashId) {
        if (jdbc.update("DELETE FROM note_trash WHERE id=? AND user_id=?", trashId, userId) == 0) {
            throw new BusinessException(404, "Trash item not found");
        }
    }

    public List<Map<String, Object>> versions(Long userId, Long noteId) {
        ownedNote(userId, noteId);
        return jdbc.queryForList("""
                SELECT id,note_id,title,version_no,created_at FROM note_versions
                WHERE user_id=? AND note_id=? ORDER BY version_no DESC
                """, userId, noteId);
    }

    @Transactional
    public void restoreVersion(Long userId, Long noteId, Long versionId) {
        ownedNote(userId, noteId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM note_versions WHERE id=? AND note_id=? AND user_id=?", versionId, noteId, userId);
        if (rows.isEmpty()) throw new BusinessException(404, "Version not found");
        Map<String, Object> row = rows.get(0);
        jdbc.update("""
                UPDATE notes SET title=?,content_md=?,category_id=?,difficulty_level=? WHERE id=? AND user_id=?
                """, row.get("title"), row.get("content_md"), row.get("category_id"),
                row.get("difficulty_level"), noteId, userId);
    }

    @Transactional
    public List<Long> importNotes(Long userId, List<Map<String, Object>> notes) {
        if (notes == null || notes.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> item : notes.stream().limit(200).toList()) {
            String title = Objects.toString(item.get("title"), "导入笔记").trim();
            String content = Objects.toString(item.get("contentMd"), Objects.toString(item.get("content"), ""));
            jdbc.update("""
                    INSERT INTO notes(user_id,title,content_md,difficulty_level,deleted)
                    VALUES(?,?,?,'medium',0)
                    """, userId, title.isEmpty() ? "导入笔记" : title, content);
            Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            ids.add(id);
            aiAgentClient.syncEmbeddingAsync(id, userId, content, title);
        }
        return ids;
    }

    public List<Map<String, Object>> exportNotes(Long userId) {
        return jdbc.queryForList("""
                SELECT id,title,content_md AS contentMd,category_id AS categoryId,
                difficulty_level AS difficultyLevel,created_at AS createdAt,updated_at AS updatedAt
                FROM notes WHERE user_id=? AND deleted=0 ORDER BY updated_at DESC
                """, userId);
    }
}
