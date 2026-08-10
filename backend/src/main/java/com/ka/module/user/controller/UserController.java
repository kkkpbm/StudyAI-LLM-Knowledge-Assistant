package com.ka.module.user.controller;

import com.ka.common.Result;
import com.ka.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    /**
     * 获取个人信息
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(Authentication auth) {
        return Result.ok(userService.getProfile(getUserId(auth)));
    }

    /**
     * 更新个人信息（邮箱、头像）
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(Authentication auth,
                                   @RequestBody Map<String, String> body) {
        userService.updateProfile(getUserId(auth), body.get("email"), body.get("avatar"));
        return Result.ok();
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(Authentication auth,
                                                     @RequestParam("file") MultipartFile file) {
        String avatar = userService.uploadAvatar(getUserId(auth), file);
        return Result.ok(Map.of("avatar", avatar));
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<?> changePassword(Authentication auth,
                                    @RequestBody Map<String, String> body) {
        userService.changePassword(getUserId(auth),
                body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    /**
     * 获取学习统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getLearningStats(Authentication auth) {
        return Result.ok(userService.getLearningStats(getUserId(auth)));
    }

    /**
     * 获取最近7天学习分布
     */
    @GetMapping("/stats/weekly")
    public Result<?> getWeeklyActivity(Authentication auth) {
        return Result.ok(userService.getWeeklyActivity(getUserId(auth)));
    }

    /**
     * 获取分类笔记分布（雷达图数据）
     */
    @GetMapping("/stats/categories")
    public Result<Map<String, Object>> getCategoryDistribution(Authentication auth) {
        return Result.ok(userService.getCategoryDistribution(getUserId(auth)));
    }
}

