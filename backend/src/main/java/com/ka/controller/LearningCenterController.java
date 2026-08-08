package com.ka.controller;

import com.ka.common.Result;
import com.ka.service.LearningCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning-center")
@RequiredArgsConstructor
public class LearningCenterController {
    private final LearningCenterService service;

    private Long userId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/flashcards/generate/{noteId}")
    public Result<List<Map<String, Object>>> generate(Authentication auth, @PathVariable Long noteId,
                                                       @RequestParam(defaultValue = "6") int count) {
        return Result.ok(service.generateFlashcards(userId(auth), noteId, count));
    }

    @GetMapping("/flashcards/due")
    public Result<List<Map<String, Object>>> due(Authentication auth,
                                                 @RequestParam(defaultValue = "30") int limit) {
        return Result.ok(service.getDueFlashcards(userId(auth), limit));
    }

    @PutMapping("/flashcards/{id}/review")
    public Result<Map<String, Object>> review(Authentication auth, @PathVariable Long id,
                                               @RequestBody Map<String, Integer> body) {
        return Result.ok(service.reviewFlashcard(userId(auth), id, body.getOrDefault("quality", 3)));
    }

    @GetMapping("/semantic-search")
    public Result<List<Map<String, Object>>> semanticSearch(Authentication auth, @RequestParam String query,
                                                             @RequestParam(defaultValue = "8") int topK) {
        return Result.ok(service.semanticSearch(userId(auth), query, topK));
    }

    @GetMapping("/weekly-report")
    public Result<Map<String, Object>> weeklyReport(Authentication auth) {
        return Result.ok(service.weeklyReport(userId(auth)));
    }

    @GetMapping("/achievements")
    public Result<List<Map<String, Object>>> achievements(Authentication auth) {
        return Result.ok(service.achievements(userId(auth)));
    }

    @GetMapping("/trash")
    public Result<List<Map<String, Object>>> trash(Authentication auth) {
        return Result.ok(service.trash(userId(auth)));
    }

    @PostMapping("/trash/{id}/restore")
    public Result<Long> restoreTrash(Authentication auth, @PathVariable Long id) {
        return Result.ok(service.restoreTrash(userId(auth), id));
    }

    @DeleteMapping("/trash/{id}")
    public Result<?> deleteTrash(Authentication auth, @PathVariable Long id) {
        service.permanentlyDeleteTrash(userId(auth), id);
        return Result.ok();
    }

    @GetMapping("/notes/{noteId}/versions")
    public Result<List<Map<String, Object>>> versions(Authentication auth, @PathVariable Long noteId) {
        return Result.ok(service.versions(userId(auth), noteId));
    }

    @PostMapping("/notes/{noteId}/versions/{versionId}/restore")
    public Result<?> restoreVersion(Authentication auth, @PathVariable Long noteId, @PathVariable Long versionId) {
        service.restoreVersion(userId(auth), noteId, versionId);
        return Result.ok();
    }

    @PostMapping("/notes/import")
    public Result<List<Long>> importNotes(Authentication auth, @RequestBody List<Map<String, Object>> notes) {
        return Result.ok(service.importNotes(userId(auth), notes));
    }

    @GetMapping("/notes/export")
    public Result<List<Map<String, Object>>> exportNotes(Authentication auth) {
        return Result.ok(service.exportNotes(userId(auth)));
    }
}
