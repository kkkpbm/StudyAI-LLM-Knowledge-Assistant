package com.ka.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.Result;
import com.ka.entity.Note;
import com.ka.entity.LearningPlan;
import com.ka.entity.LearningRecord;
import com.ka.entity.ReviewReminder;
import com.ka.entity.Category;
import com.ka.mapper.*;
import com.ka.service.KnowledgeRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final NoteMapper noteMapper;
    private final LearningPlanMapper planMapper;
    private final LearningRecordMapper recordMapper;
    private final ReviewReminderMapper reminderMapper;
    private final CategoryMapper categoryMapper;
    private final KnowledgeRelationService relationService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(Authentication auth) {
        Long userId = getUserId(auth);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("noteCount", noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId)));
        data.put("planCount", planMapper.selectCount(new LambdaQueryWrapper<LearningPlan>().eq(LearningPlan::getUserId, userId).eq(LearningPlan::getStatus, 1)));
        // 计算今日学习时长
        LocalDate today = LocalDate.now();
        List<LearningRecord> todayRecords = recordMapper.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getRecordedAt, today.atStartOfDay()));
        long todayMinutes = todayRecords.stream().mapToLong(r -> r.getDurationMinutes() == null ? 0 : r.getDurationMinutes()).sum();
        data.put("todayMinutes", todayMinutes);
        data.put("reviewCount", reminderMapper.selectCount(new LambdaQueryWrapper<ReviewReminder>()
                .eq(ReviewReminder::getUserId, userId).le(ReviewReminder::getNextReviewAt, LocalDate.now())));
        data.put("conceptCount", relationService.getConceptCount(userId));
        return Result.ok(data);
    }

    @GetMapping("/category-distribution")
    public Result<List<Map<String, Object>>> categoryDistribution(Authentication auth) {
        Long userId = getUserId(auth);
        // 获取用户所有分类
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId));
        // 获取所有笔记
        List<Note> notes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));

        // 统计每个分类的笔记数
        Map<Long, Long> countMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> colorMap = new HashMap<>();
        for (Category c : categories) {
            nameMap.put(c.getId(), c.getName());
            colorMap.put(c.getId(), c.getColor());
            countMap.put(c.getId(), 0L);
        }
        for (Note n : notes) {
            if (n.getCategoryId() != null) {
                countMap.merge(n.getCategoryId(), 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Category c : categories) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", c.getName());
            item.put("value", countMap.getOrDefault(c.getId(), 0L));
            item.put("color", c.getColor());
            result.add(item);
        }
        return Result.ok(result);
    }
}
