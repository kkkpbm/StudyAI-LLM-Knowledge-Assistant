package com.ka.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.BusinessException;
import com.ka.common.Result;
import com.ka.entity.LearningPlan;
import com.ka.entity.Note;
import com.ka.entity.PlanItem;
import com.ka.mapper.LearningPlanMapper;
import com.ka.mapper.NoteMapper;
import com.ka.mapper.PlanItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class LearningPlanController {

    private final LearningPlanMapper planMapper;
    private final PlanItemMapper itemMapper;
    private final NoteMapper noteMapper;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    private LearningPlan requireOwnedPlan(Long id, Long userId) {
        LearningPlan plan = planMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(404, "Learning plan not found");
        }
        return plan;
    }

    private PlanItem requireOwnedItem(Long planId, Long itemId, Long userId) {
        requireOwnedPlan(planId, userId);
        PlanItem item = itemMapper.selectOne(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getId, itemId)
                .eq(PlanItem::getPlanId, planId));
        if (item == null) {
            throw new BusinessException(404, "Plan item not found");
        }
        return item;
    }

    private void requireOwnedNote(Long noteId, Long userId) {
        if (noteId == null) return;
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) {
            throw new BusinessException(404, "Note not found");
        }
    }

    @GetMapping
    public Result<List<LearningPlan>> list(Authentication auth) {
        return Result.ok(planMapper.selectList(new LambdaQueryWrapper<LearningPlan>()
                .eq(LearningPlan::getUserId, getUserId(auth)).orderByDesc(LearningPlan::getCreatedAt)));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(Authentication auth, @PathVariable Long id) {
        LearningPlan plan = requireOwnedPlan(id, getUserId(auth));
        List<PlanItem> items = itemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, id).orderByAsc(PlanItem::getOrderNum));
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("plan", plan);
        result.put("items", items);
        return Result.ok(result);
    }

    @PostMapping
    public Result<LearningPlan> create(Authentication auth, @RequestBody LearningPlan plan) {
        plan.setId(null);
        plan.setUserId(getUserId(auth));
        planMapper.insert(plan);
        return Result.ok(plan);
    }

    @PutMapping("/{id}")
    public Result<?> update(Authentication auth, @PathVariable Long id, @RequestBody LearningPlan plan) {
        Long userId = getUserId(auth);
        requireOwnedPlan(id, userId);
        plan.setId(id);
        plan.setUserId(userId);
        planMapper.updateById(plan);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(Authentication auth, @PathVariable Long id) {
        requireOwnedPlan(id, getUserId(auth));
        itemMapper.delete(new LambdaQueryWrapper<PlanItem>().eq(PlanItem::getPlanId, id));
        planMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/{id}/items")
    public Result<List<PlanItem>> items(Authentication auth, @PathVariable Long id) {
        requireOwnedPlan(id, getUserId(auth));
        return Result.ok(itemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, id).orderByAsc(PlanItem::getOrderNum)));
    }

    @PostMapping("/{id}/items")
    public Result<PlanItem> addItem(Authentication auth, @PathVariable Long id, @RequestBody PlanItem item) {
        Long userId = getUserId(auth);
        requireOwnedPlan(id, userId);
        requireOwnedNote(item.getNoteId(), userId);
        item.setId(null);
        item.setPlanId(id);
        itemMapper.insert(item);
        return Result.ok(item);
    }

    @PutMapping("/{id}/items/{itemId}/complete")
    public Result<?> completeItem(Authentication auth, @PathVariable Long id, @PathVariable Long itemId) {
        PlanItem item = requireOwnedItem(id, itemId, getUserId(auth));
        item.setCompleted(true);
        item.setCompletedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        return Result.ok();
    }
}
