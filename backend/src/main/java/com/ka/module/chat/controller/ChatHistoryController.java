package com.ka.module.chat.controller;

import com.ka.common.Result;
import com.ka.module.chat.domain.ChatHistory;
import com.ka.module.chat.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-history")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<ChatHistory>> list(Authentication auth,
                                          @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(chatHistoryService.listByUser(getUserId(auth), limit));
    }

    @PostMapping
    public Result<ChatHistory> save(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = getUserId(auth);
        String role = body.get("role").toString();
        String content = body.get("content").toString();
        Long noteId = body.get("noteId") != null ? Long.valueOf(body.get("noteId").toString()) : null;
        return Result.ok(chatHistoryService.save(userId, role, content, noteId));
    }

    @DeleteMapping
    public Result<?> clear(Authentication auth) {
        chatHistoryService.clearByUser(getUserId(auth));
        return Result.ok();
    }
}

