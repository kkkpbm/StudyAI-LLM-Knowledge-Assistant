package com.ka.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.Result;
import com.ka.entity.LearningRecord;
import com.ka.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class LearningRecordController {

    private final LearningRecordMapper recordMapper;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/check-in")
    public Result<LearningRecord> checkIn(Authentication auth, @RequestBody LearningRecord record) {
        record.setUserId(getUserId(auth));
        record.setRecordedAt(LocalDateTime.now());
        recordMapper.insert(record);
        return Result.ok(record);
    }

    @GetMapping("/calendar")
    public Result<Map<String, Integer>> calendar(Authentication auth,
                                                  @RequestParam String month) {
        Long userId = getUserId(auth);
        List<LearningRecord> records = recordMapper.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .likeRight(LearningRecord::getRecordedAt, month));

        Map<String, Integer> heatmap = new HashMap<>();
        for (LearningRecord r : records) {
            String day = r.getRecordedAt().toLocalDate().toString();
            int minutes = r.getDurationMinutes() != null ? r.getDurationMinutes() : 0;
            heatmap.merge(day, minutes, Integer::sum);
        }
        return Result.ok(heatmap);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(Authentication auth) {
        Long userId = getUserId(auth);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);

        List<LearningRecord> weekRecords = recordMapper.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getRecordedAt, weekStart.atStartOfDay()));

        long totalMinutes = weekRecords.stream().mapToLong(r -> r.getDurationMinutes() == null ? 0 : r.getDurationMinutes()).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayMinutes", 0);
        stats.put("weekMinutes", totalMinutes);
        stats.put("streak", 0);
        stats.put("totalRecords", recordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)));
        return Result.ok(stats);
    }
}
