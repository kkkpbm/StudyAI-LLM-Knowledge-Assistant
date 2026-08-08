package com.ka.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    Map<String, Object> getProfile(Long userId);

    void updateProfile(Long userId, String email, String avatar);

    String uploadAvatar(Long userId, MultipartFile file);

    void changePassword(Long userId, String oldPassword, String newPassword);

    Map<String, Object> getLearningStats(Long userId);

    List<Map<String, Object>> getWeeklyActivity(Long userId);

    Map<String, Object> getCategoryDistribution(Long userId);
}
