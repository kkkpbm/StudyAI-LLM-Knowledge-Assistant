package com.ka.service;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface AiAgentClient {
    Object summarize(String contentMd);

    Object chat(Long userId, Long noteId, String question, List<Map<String, String>> history, String mode);

    Flux<String> chatStream(Long userId, Long noteId, String question, List<Map<String, String>> history, String mode);

    Object genPlan(String goal);

    Object extractGraph(String contentMd);

    Object assessDifficulty(String contentMd);

    Object suggestTags(String content);

    Object learningInsight(Map<String, Object> data);

    Object syncEmbedding(Long noteId, Long userId, String contentMd, String title);

    void syncEmbeddingAsync(Long noteId, Long userId, String contentMd, String title);

    void deleteEmbedding(Long noteId);

    void deleteEmbeddingAsync(Long noteId);

    Object syncChatMemory(Long userId, String question, String answer);

    void syncChatMemoryAsync(Long userId, String question, String answer);

    Object generateFlashcards(String title, String content, int count);

    /** 将上传的学习资料交由 AI 服务提取文本并生成结构化笔记草稿。 */
    java.util.Map<String, Object> parseDocument(byte[] fileBytes, String fileName, String contentType);

    Object searchNotes(Long userId, String query, int topK);
}
