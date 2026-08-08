package com.ka.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ka.service.AiAgentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.client.MultipartBodyBuilder;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentClientImpl implements AiAgentClient {

    @Value("${ai-agent.base-url}")
    private String baseUrl;

    private WebClient client;

    private WebClient getClient() {
        if (client == null) {
            client = WebClient.builder().baseUrl(baseUrl).build();
        }
        return client;
    }

    public Object summarize(String contentMd) {
        String response = getClient().post().uri("/agent/summarize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", contentMd))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Object chat(Long userId, Long noteId, String question, java.util.List<Map<String, String>> history, String mode) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("user_id", userId);
        body.put("question", question);
        if (noteId != null) {
            body.put("note_id", noteId);
        }
        if (history != null) {
            body.put("history", history);
        }
        body.put("mode", mode);
        String response = getClient().post().uri("/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Flux<String> chatStream(Long userId, Long noteId, String question, java.util.List<Map<String, String>> history, String mode) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("user_id", userId);
        body.put("question", question);
        if (noteId != null) {
            body.put("note_id", noteId);
        }
        if (history != null) {
            body.put("history", history);
        }
        body.put("mode", mode);
        return getClient().post().uri("/agent/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class);
    }
    
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(response, java.util.Map.class);
        } catch (Exception e) {
            return java.util.Map.of("raw_response", response);
        }
    }

    public Object genPlan(String goal) {
        String response = getClient().post().uri("/agent/gen-learning-plan")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("goal", goal))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Object extractGraph(String contentMd) {
        String response = getClient().post().uri("/agent/extract-graph")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", contentMd))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Object assessDifficulty(String contentMd) {
        String response = getClient().post().uri("/agent/assess-difficulty")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", contentMd))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Object suggestTags(String content) {
        String response = getClient().post().uri("/agent/suggest-tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", content))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Object learningInsight(Map<String, Object> data) {
        String response = getClient().post().uri("/agent/learning-insight")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Object syncEmbedding(Long noteId, Long userId, String contentMd, String title) {
        return getClient().post().uri("/agent/embeddings/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "note_id", noteId,
                        "user_id", userId,
                        "content", contentMd == null ? "" : contentMd,
                        "title", title == null ? "" : title))
                .retrieve().bodyToMono(Object.class).block();
    }

    @Async
    public void syncEmbeddingAsync(Long noteId, Long userId, String contentMd, String title) {
        try {
            syncEmbedding(noteId, userId, contentMd, title);
        } catch (Exception e) {
            log.warn("Failed to sync embedding for note {}: {}", noteId, e.getMessage());
        }
    }

    public void deleteEmbedding(Long noteId) {
        getClient().delete().uri("/agent/embeddings/" + noteId)
                .retrieve().toBodilessEntity().block();
    }

    @Async
    public void deleteEmbeddingAsync(Long noteId) {
        try {
            deleteEmbedding(noteId);
        } catch (Exception e) {
            log.warn("Failed to delete embedding for note {}: {}", noteId, e.getMessage());
        }
    }

    public Object syncChatMemory(Long userId, String question, String answer) {
        String response = getClient().post().uri("/agent/chat-memory/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("user_id", userId, "question", question, "answer", answer))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    @Async
    public void syncChatMemoryAsync(Long userId, String question, String answer) {
        try {
            syncChatMemory(userId, question, answer);
        } catch (Exception e) {
            log.warn("Failed to sync chat memory: {}", e.getMessage());
        }
    }

    public Object generateFlashcards(String title, String content, int count) {
        String response = getClient().post().uri("/agent/generate-flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "title", title == null ? "" : title,
                        "content", content == null ? "" : content,
                        "count", Math.max(1, Math.min(count, 12))))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseDocument(byte[] fileBytes, String fileName, String contentType) {
        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override public String getFilename() { return fileName; }
        };
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", resource)
                .filename(fileName)
                .contentType(contentType == null || contentType.isBlank()
                        ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType));
        String response = getClient().post().uri("/agent/documents/parse")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }

    public Object searchNotes(Long userId, String query, int topK) {
        String response = getClient().post().uri("/agent/search-notes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "user_id", userId,
                        "query", query,
                        "top_k", Math.max(1, Math.min(topK, 20))))
                .retrieve().bodyToMono(String.class).block();
        return parseJsonResponse(response);
    }
}
