package com.ka.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.BusinessException;
import com.ka.module.knowledge.service.KnowledgeRelationService;
import com.ka.module.learning.domain.LearningPlan;
import com.ka.module.learning.domain.LearningRecord;
import com.ka.module.learning.mapper.LearningPlanMapper;
import com.ka.module.learning.mapper.LearningRecordMapper;
import com.ka.module.knowledge.domain.Category;
import com.ka.module.knowledge.domain.Note;
import com.ka.module.knowledge.mapper.CategoryMapper;
import com.ka.module.knowledge.mapper.NoteMapper;
import com.ka.module.user.domain.User;
import com.ka.module.user.mapper.UserMapper;
import com.ka.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    private final LearningPlanMapper planMapper;
    private final LearningRecordMapper recordMapper;
    private final CategoryMapper categoryMapper;
    private final KnowledgeRelationService relationService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户完整信息（不含密码）
     */
    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "User not found");

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("avatar", user.getAvatar());
        profile.put("role", user.getRole());
        profile.put("createdAt", user.getCreatedAt());
        return profile;
    }

    /**
     * 更新用户信息（邮箱、头像）
     */
    public void updateProfile(Long userId, String email, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "User not found");
        if (email != null) user.setEmail(email);
        if (avatar != null) user.setAvatar(avatar);
        userMapper.updateById(user);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择头像文件");
        }

        String contentType = file.getContentType();
        Map<String, String> extensions = Map.of(
                "image/jpeg", ".jpg",
                "image/png", ".png",
                "image/webp", ".webp"
        );
        String extension = extensions.get(contentType);
        if (extension == null) {
            throw new BusinessException(400, "头像仅支持 JPG、PNG 或 WebP 格式");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(400, "头像大小不能超过 2MB");
        }

        try {
            Path avatarDirectory = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
            Files.createDirectories(avatarDirectory);
            String filename = userId + "-" + UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), avatarDirectory.resolve(filename));

            String avatarUrl = "/uploads/avatars/" + filename;
            updateProfile(userId, null, avatarUrl);
            return avatarUrl;
        } catch (IOException e) {
            throw new BusinessException(500, "头像保存失败，请稍后重试");
        }
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "User not found");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 获取用户学习统计
     */
    public Map<String, Object> getLearningStats(Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 笔记总数
        long noteCount = noteMapper.selectCount(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));
        stats.put("noteCount", noteCount);

        // 计划总数
        long planCount = planMapper.selectCount(
                new LambdaQueryWrapper<LearningPlan>().eq(LearningPlan::getUserId, userId));
        stats.put("planCount", planCount);

        // 总学习时长（分钟）
        List<LearningRecord> allRecords = recordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getUserId, userId));
        long totalMinutes = allRecords.stream()
                .mapToLong(r -> r.getDurationMinutes() == null ? 0 : r.getDurationMinutes()).sum();
        stats.put("totalMinutes", totalMinutes);

        // 概念数量
        long conceptCount = relationService.getConceptCount(userId);
        stats.put("conceptCount", conceptCount);

        // 连续学习天数
        int streak = calcStreak(allRecords);
        stats.put("streak", streak);

        return stats;
    }

    /**
     * 获取最近7天学习时长分布
     */
    public List<Map<String, Object>> getWeeklyActivity(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<LearningRecord> dayRecords = recordMapper.selectList(
                    new LambdaQueryWrapper<LearningRecord>()
                            .eq(LearningRecord::getUserId, userId)
                            .ge(LearningRecord::getRecordedAt, date.atStartOfDay())
                            .lt(LearningRecord::getRecordedAt, date.plusDays(1).atStartOfDay()));
            long minutes = dayRecords.stream()
                    .mapToLong(r -> r.getDurationMinutes() == null ? 0 : r.getDurationMinutes()).sum();

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("dayOfWeek", dayOfWeek(date.getDayOfWeek().getValue()));
            day.put("minutes", minutes);
            result.add(day);
        }
        return result;
    }

    /**
     * 获取分类笔记分布（用于雷达图）
     */
    public Map<String, Object> getCategoryDistribution(Long userId) {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId));
        List<Note> notes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));

        List<String> names = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (Category cat : categories) {
            names.add(cat.getName());
            long count = notes.stream().filter(n -> cat.getId().equals(n.getCategoryId())).count();
            values.add((int) count);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", names);
        result.put("values", values);
        return result;
    }

    private int calcStreak(List<LearningRecord> records) {
        if (records.isEmpty()) return 0;
        Set<LocalDate> dates = new HashSet<>();
        for (LearningRecord r : records) {
            LocalDateTime recordedAt = r.getRecordedAt();
            if (recordedAt != null) {
                dates.add(recordedAt.toLocalDate());
            }
        }
        int streak = 0;
        LocalDate today = LocalDate.now();
        // 今天或昨天有学习记录才计入连续
        if (!dates.contains(today) && !dates.contains(today.minusDays(1))) {
            return 0;
        }
        LocalDate check = dates.contains(today) ? today : today.minusDays(1);
        while (dates.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        return streak;
    }

    private String dayOfWeek(int value) {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return days[value - 1];
    }
}

