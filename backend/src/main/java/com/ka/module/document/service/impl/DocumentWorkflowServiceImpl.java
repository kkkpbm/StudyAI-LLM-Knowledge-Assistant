package com.ka.module.document.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ka.common.BusinessException;
import com.ka.module.knowledge.domain.NoteDTO;
import com.ka.module.knowledge.domain.Note;
import com.ka.module.ai.service.AiAgentClient;
import com.ka.module.document.service.DocumentWorkflowService;
import com.ka.module.knowledge.service.NoteService;
import com.ka.module.learning.service.ReviewReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentWorkflowServiceImpl implements DocumentWorkflowService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md", "markdown");
    private static final long MAX_SIZE = 15L * 1024 * 1024;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AiAgentClient aiAgentClient;
    private final NoteService noteService;
    private final ReviewReminderService reviewReminderService;

    @Override
    public Map<String, Object> parse(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(400, "请选择需要解析的文档");
        if (file.getSize() > MAX_SIZE) throw new BusinessException(400, "单个文档不能超过 15MB");
        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("untitled.txt");
        String extension = extensionOf(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) throw new BusinessException(400, "仅支持 PDF、DOCX、TXT 和 Markdown 文档");

        jdbc.update("""
                INSERT INTO document_workflows(user_id,file_name,file_type,file_size,status)
                VALUES(?,?,?,?,?)
                """, userId, fileName, extension, file.getSize(), "UPLOADED");
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("UPDATE document_workflows SET status=? WHERE id=?", "EXTRACTING", id);
        try {
            jdbc.update("UPDATE document_workflows SET status=? WHERE id=?", "ANALYZING", id);
            Map<String, Object> draft = aiAgentClient.parseDocument(file.getBytes(), fileName, file.getContentType());
            String draftJson = objectMapper.writeValueAsString(draft);
            jdbc.update("UPDATE document_workflows SET status=?,draft_json=? WHERE id=?", "REVIEWING", draftJson, id);
            return workflow(id, userId);
        } catch (Exception e) {
            jdbc.update("UPDATE document_workflows SET status=?,error_message=? WHERE id=?", "FAILED", messageOf(e), id);
            throw new BusinessException(502, "文档解析失败：" + messageOf(e));
        }
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirm(Long userId, Long workflowId, Map<String, Object> body) {
        Map<String, Object> task = getWorkflow(workflowId, userId);
        if (!"REVIEWING".equals(task.get("status"))) throw new BusinessException(400, "该解析任务暂不可确认入库");
        try {
            Map<String, Object> draft = objectMapper.readValue(Objects.toString(task.get("draft_json"), "{}"), new TypeReference<>() {});
            NoteDTO dto = new NoteDTO();
            dto.setTitle(Objects.toString(body.getOrDefault("title", draft.getOrDefault("title", "导入资料"))));
            dto.setContentMd(Objects.toString(body.getOrDefault("contentMd", draft.getOrDefault("content_md", ""))));
            dto.setDifficultyLevel(Objects.toString(body.getOrDefault("difficultyLevel", draft.getOrDefault("difficulty_level", "intermediate"))));
            if (body.get("categoryId") != null) dto.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
            if (body.get("tagIds") instanceof List<?> tags) {
                dto.setTagIds(tags.stream().map(item -> Long.valueOf(item.toString())).toList());
            }
            Note note = noteService.create(userId, dto);
            aiAgentClient.syncEmbeddingAsync(note.getId(), userId, dto.getContentMd(), dto.getTitle());
            reviewReminderService.create(userId, note.getId());
            jdbc.update("UPDATE document_workflows SET status=?,note_id=? WHERE id=?", "COMPLETED", note.getId(), workflowId);
            return Map.of("workflow", workflow(workflowId, userId), "note", note);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "创建笔记失败：" + messageOf(e));
        }
    }

    @Override
    public List<Map<String, Object>> list(Long userId) {
        return jdbc.queryForList("""
                SELECT id,file_name,file_type,file_size,status,error_message,note_id,created_at,updated_at
                FROM document_workflows WHERE user_id=? ORDER BY id DESC LIMIT 30
                """, userId);
    }

    private Map<String, Object> workflow(Long id, Long userId) {
        Map<String, Object> task = getWorkflow(id, userId);
        try {
            if (task.get("draft_json") != null) task.put("draft", objectMapper.readValue(task.get("draft_json").toString(), new TypeReference<Map<String, Object>>() {}));
        } catch (Exception ignored) { }
        task.remove("draft_json");
        return task;
    }

    private Map<String, Object> getWorkflow(Long id, Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM document_workflows WHERE id=? AND user_id=?", id, userId);
        if (rows.isEmpty()) throw new BusinessException(404, "解析任务不存在");
        return new LinkedHashMap<>(rows.get(0));
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String messageOf(Exception e) {
        String message = Optional.ofNullable(e.getMessage()).orElse("未知错误").replaceAll("[\\r\\n]", " ");
        return message.substring(0, Math.min(420, message.length()));
    }
}

