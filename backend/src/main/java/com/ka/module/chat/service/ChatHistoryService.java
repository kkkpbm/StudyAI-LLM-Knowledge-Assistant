package com.ka.module.chat.service;

import com.ka.module.chat.domain.ChatHistory;

import java.util.List;

public interface ChatHistoryService {
    ChatHistory save(Long userId, String role, String content, Long noteId);

    List<ChatHistory> listByUser(Long userId, int limit);

    void clearByUser(Long userId);
}

