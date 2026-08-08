package com.ka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.entity.ChatHistory;
import com.ka.mapper.ChatHistoryMapper;
import com.ka.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatHistoryMapper chatHistoryMapper;

    public ChatHistory save(Long userId, String role, String content, Long noteId) {
        ChatHistory chat = new ChatHistory();
        chat.setUserId(userId);
        chat.setRole(role);
        chat.setContent(content);
        chat.setNoteId(noteId);
        chatHistoryMapper.insert(chat);
        return chat;
    }

    public List<ChatHistory> listByUser(Long userId, int limit) {
        return chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .orderByAsc(ChatHistory::getCreatedAt)
                        .last("LIMIT " + limit)
        );
    }

    public void clearByUser(Long userId) {
        chatHistoryMapper.delete(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getUserId, userId));
    }
}
