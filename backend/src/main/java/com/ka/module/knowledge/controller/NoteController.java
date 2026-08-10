package com.ka.module.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ka.common.Result;
import com.ka.module.knowledge.domain.NoteDTO;
import com.ka.module.knowledge.domain.Note;
import com.ka.module.knowledge.domain.Tag;
import com.ka.module.ai.service.AiAgentClient;
import com.ka.module.knowledge.service.KnowledgeRelationService;
import com.ka.module.knowledge.service.NoteService;
import com.ka.module.learning.service.ReviewReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final AiAgentClient aiAgentClient;
    private final KnowledgeRelationService relationService;
    private final ReviewReminderService reviewReminderService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<IPage<Note>> list(Authentication auth,
                                    @RequestParam(defaultValue = "") String keyword,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return Result.ok(noteService.page(getUserId(auth), categoryId, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<Note> detail(Authentication auth, @PathVariable Long id) {
        return Result.ok(noteService.getById(id, getUserId(auth)));
    }

    @PostMapping
    public Result<Note> create(Authentication auth, @Valid @RequestBody NoteDTO dto) {
        Long userId = getUserId(auth);
        Note note = noteService.create(userId, dto);
        aiAgentClient.syncEmbeddingAsync(note.getId(), userId, dto.getContentMd(), dto.getTitle());
        reviewReminderService.create(userId, note.getId());
        return Result.ok(note);
    }

    @PutMapping("/{id}")
    public Result<Note> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody NoteDTO dto) {
        Long userId = getUserId(auth);
        Note note = noteService.update(userId, id, dto);
        aiAgentClient.syncEmbeddingAsync(id, userId, dto.getContentMd(), dto.getTitle());
        return Result.ok(note);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(Authentication auth, @PathVariable Long id) {
        Long userId = getUserId(auth);
        relationService.deleteRelationsByNoteId(userId, id);
        noteService.delete(userId, id);
        aiAgentClient.deleteEmbeddingAsync(id);
        return Result.ok();
    }

    @GetMapping("/{id}/tags")
    public Result<List<Tag>> tags(Authentication auth, @PathVariable Long id) {
        return Result.ok(noteService.getTags(getUserId(auth), id));
    }
}

