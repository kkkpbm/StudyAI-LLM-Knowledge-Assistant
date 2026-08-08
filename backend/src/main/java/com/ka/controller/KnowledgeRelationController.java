package com.ka.controller;

import com.ka.common.Result;
import com.ka.entity.KnowledgeRelation;
import com.ka.service.KnowledgeRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeRelationController {

    private final KnowledgeRelationService relationService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    /**
     * 构建知识图谱：分析所有笔记，提取概念关系并保存
     */
    @PostMapping("/build")
    public Result<Map<String, Object>> buildGraph(Authentication auth) {
        Long userId = getUserId(auth);
        Map<String, Object> result = relationService.buildGraphForAllNotes(userId);
        return Result.ok(result);
    }

    /**
     * 获取所有知识关系
     */
    @GetMapping("/relations")
    public Result<List<KnowledgeRelation>> getAllRelations(Authentication auth) {
        Long userId = getUserId(auth);
        return Result.ok(relationService.getAllRelations(userId));
    }

    /**
     * 获取指定笔记的知识关系
     */
    @GetMapping("/relations/{noteId}")
    public Result<List<KnowledgeRelation>> getRelationsByNoteId(Authentication auth, @PathVariable Long noteId) {
        return Result.ok(relationService.getRelationsByNoteId(getUserId(auth), noteId));
    }

    /**
     * 删除所有知识关系
     */
    @DeleteMapping("/relations")
    public Result<?> deleteAllRelations(Authentication auth) {
        Long userId = getUserId(auth);
        relationService.deleteAllRelations(userId);
        return Result.ok();
    }

    /**
     * 删除指定笔记的知识关系
     */
    @DeleteMapping("/relations/{noteId}")
    public Result<?> deleteRelationsByNoteId(Authentication auth, @PathVariable Long noteId) {
        relationService.deleteRelationsByNoteId(getUserId(auth), noteId);
        return Result.ok();
    }

    /**
     * 获取概念数量统计
     */
    @GetMapping("/concepts/count")
    public Result<Map<String, Object>> getConceptCount(Authentication auth) {
        Long userId = getUserId(auth);
        long count = relationService.getConceptCount(userId);
        return Result.ok(Map.of("count", count));
    }

    /**
     * 获取概念详情：关联关系、连接概念、相关笔记
     */
    @GetMapping("/concepts/{conceptName}")
    public Result<Map<String, Object>> getConceptDetail(Authentication auth,
                                                         @PathVariable String conceptName) {
        Long userId = getUserId(auth);
        return Result.ok(relationService.getConceptDetail(userId, conceptName));
    }

    @PostMapping("/relations/manual")
    public Result<KnowledgeRelation> createRelation(Authentication auth, @RequestBody Map<String, Object> body) {
        return Result.ok(relationService.createRelation(
                getUserId(auth), Long.valueOf(body.get("noteId").toString()),
                String.valueOf(body.get("source")), String.valueOf(body.get("target")),
                String.valueOf(body.getOrDefault("relationType", "related")),
                body.get("weight") instanceof Number number ? number.doubleValue() : 0.5));
    }

    @PutMapping("/relations/manual/{id}")
    public Result<KnowledgeRelation> updateRelation(Authentication auth, @PathVariable Long id,
                                                     @RequestBody Map<String, Object> body) {
        return Result.ok(relationService.updateRelation(
                getUserId(auth), id,
                body.get("source") == null ? null : body.get("source").toString(),
                body.get("target") == null ? null : body.get("target").toString(),
                body.get("relationType") == null ? null : body.get("relationType").toString(),
                body.get("weight") instanceof Number number ? number.doubleValue() : null));
    }

    @DeleteMapping("/relations/manual/{id}")
    public Result<?> deleteRelation(Authentication auth, @PathVariable Long id) {
        relationService.deleteRelation(getUserId(auth), id);
        return Result.ok();
    }

    @GetMapping("/recommendations")
    public Result<List<Map<String, Object>>> recommendations(Authentication auth) {
        return Result.ok(relationService.recommendRelations(getUserId(auth)));
    }
}
