package com.ka.service;

import com.ka.entity.ChatHistory;

import java.util.List;

public interface ChatHistoryService {
    ChatHistory save(Long userId, String role, String content, Long noteId);

    List<ChatHistory> listByUser(Long userId, int limit);

    void clearByUser(Long userId);
}
