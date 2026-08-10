package com.ka.module.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.module.ai.service.AiAgentClient;
import com.ka.common.Result;
import com.ka.module.knowledge.service.KnowledgeRelationService;
import com.ka.module.learning.domain.LearningPlan;
import com.ka.module.learning.domain.LearningRecord;
import com.ka.module.learning.domain.ReviewReminder;
import com.ka.module.learning.mapper.LearningPlanMapper;
import com.ka.module.learning.mapper.LearningRecordMapper;
import com.ka.module.learning.mapper.ReviewReminderMapper;
import com.ka.module.knowledge.domain.Category;
import com.ka.module.knowledge.domain.Note;
import com.ka.module.knowledge.mapper.CategoryMapper;
import com.ka.module.knowledge.mapper.NoteMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAgentClient aiAgentClient;
    private final NoteMapper noteMapper;
    private final LearningPlanMapper planMapper;
    private final ReviewReminderMapper reminderMapper;
    private final LearningRecordMapper recordMapper;
    private final CategoryMapper categoryMapper;
    private final KnowledgeRelationService relationService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/summarize")
    public Result<Object> summarize(@RequestBody Map<String, String> body) {
        return Result.ok(aiAgentClient.summarize(body.get("content")));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/chat")
    public Result<Object> chat(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = getUserId(auth);
        Long noteId = body.get("noteId") != null ? Long.valueOf(body.get("noteId").toString()) : null;
        String question = body.get("question").toString();
        java.util.List<Map<String, String>> history = (java.util.List<Map<String, String>>) body.get("history");
        String mode = "chat".equals(body.get("mode")) ? "chat" : "knowledge";
        return Result.ok(aiAgentClient.chat(userId, noteId, question, history, mode));
    }

    @PostMapping("/gen-plan")
    public Result<Object> genPlan(@RequestBody Map<String, String> body) {
        return Result.ok(aiAgentClient.genPlan(body.get("goal")));
    }

    @PostMapping("/extract-graph")
    public Result<Object> extractGraph(@RequestBody Map<String, String> body) {
        return Result.ok(aiAgentClient.extractGraph(body.get("content")));
    }

    @PostMapping("/assess")
    public Result<Object> assess(@RequestBody Map<String, String> body) {
        return Result.ok(aiAgentClient.assessDifficulty(body.get("content")));
    }

    @PostMapping("/suggest-tags")
    public Result<Object> suggestTags(@RequestBody Map<String, String> body) {
        return Result.ok(aiAgentClient.suggestTags(body.get("content")));
    }

    @PostMapping("/chat-memory/sync")
    public Result<?> syncChatMemory(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = getUserId(auth);
        String question = body.get("question").toString();
        String answer = body.get("answer").toString();
        aiAgentClient.syncChatMemoryAsync(userId, question, answer);
        return Result.ok();
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/chat/stream")
    public void chatStream(Authentication auth, @RequestBody Map<String, Object> body, HttpServletResponse response) {
        Long userId = getUserId(auth);
        Long noteId = body.get("noteId") != null ? Long.valueOf(body.get("noteId").toString()) : null;
        String question = body.get("question").toString();
        java.util.List<Map<String, String>> history = (java.util.List<Map<String, String>>) body.get("history");
        String mode = "chat".equals(body.get("mode")) ? "chat" : "knowledge";

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        try {
            PrintWriter writer = response.getWriter();
            aiAgentClient.chatStream(userId, noteId, question, history, mode)
                    .doOnNext(data -> {
                        String line = data.trim();
                        if (line.isEmpty()) return;
                        if (line.startsWith("data: ")) {
                            line = line.substring(6);
                        }
                        writer.write("data: " + line + "\n\n");
                        writer.flush();
                    })
                    .doOnError(e -> {
                        writer.write("data: [ERROR] " + e.getMessage() + "\n\n");
                        writer.flush();
                    })
                    .blockLast();
        } catch (Exception e) {
            // response already committed, can't send error
        }
    }

    /** 学情诊断：聚合学习数据后交由 AI 分析 */
    @GetMapping("/learning-insight")
    public Result<Object> learningInsight(Authentication auth) {
        Long userId = getUserId(auth);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("note_count", noteMapper.selectCount(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId)));
        data.put("plan_count", planMapper.selectCount(
                new LambdaQueryWrapper<LearningPlan>().eq(LearningPlan::getUserId, userId)
                        .eq(LearningPlan::getStatus, 1)));

        long todayMinutes = recordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getUserId, userId)
                        .ge(LearningRecord::getRecordedAt, LocalDate.now().atStartOfDay()))
                .stream().mapToLong(r -> r.getDurationMinutes() == null ? 0 : r.getDurationMinutes()).sum();
        data.put("today_minutes", todayMinutes);

        data.put("concept_count", relationService.getConceptCount(userId));

        long dueReviews = reminderMapper.selectCount(
                new LambdaQueryWrapper<ReviewReminder>().eq(ReviewReminder::getUserId, userId)
                        .le(ReviewReminder::getNextReviewAt, LocalDate.now()));
        data.put("review_count", dueReviews);

        // 未来 3 天待复习列表
        List<ReviewReminder> upcoming = reminderMapper.selectList(
                new LambdaQueryWrapper<ReviewReminder>().eq(ReviewReminder::getUserId, userId)
                        .le(ReviewReminder::getNextReviewAt, LocalDate.now().plusDays(3))
                        .ge(ReviewReminder::getNextReviewAt, LocalDate.now()));
        data.put("upcoming_reviews", upcoming.isEmpty() ? "无" :
                upcoming.stream().map(r -> "笔记#" + r.getNoteId() + "(" + r.getNextReviewAt() + ")")
                        .collect(Collectors.joining(", ")));

        // 最近学习主题
        List<Note> recentNotes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId)
                        .orderByDesc(Note::getUpdatedAt).last("LIMIT 5"));
        data.put("recent_topics", recentNotes.isEmpty() ? "无" :
                recentNotes.stream().map(Note::getTitle).collect(Collectors.joining(", ")));

        // 分类分布
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId));
        Map<Long, String> catNames = new HashMap<>();
        for (Category c : categories) catNames.put(c.getId(), c.getName());
        Map<String, Long> catCount = new LinkedHashMap<>();
        for (Note n : noteMapper.selectList(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId))) {
            if (n.getCategoryId() != null) {
                String name = catNames.getOrDefault(n.getCategoryId(), "未分类");
                catCount.merge(name, 1L, Long::sum);
            }
        }
        data.put("categories", catCount.isEmpty() ? "无" :
                catCount.entrySet().stream().map(e -> e.getKey() + "(" + e.getValue() + "篇)")
                        .collect(Collectors.joining(", ")));

        return Result.ok(aiAgentClient.learningInsight(data));
    }
}

