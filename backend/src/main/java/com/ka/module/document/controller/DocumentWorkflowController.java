package com.ka.module.document.controller;

import com.ka.common.Result;
import com.ka.module.document.service.DocumentWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-workflows")
@RequiredArgsConstructor
public class DocumentWorkflowController {
    private final DocumentWorkflowService service;
    private Long userId(Authentication auth) { return (Long) auth.getPrincipal(); }

    @PostMapping("/parse")
    public Result<Map<String, Object>> parse(Authentication auth, @RequestParam("file") MultipartFile file) {
        return Result.ok(service.parse(userId(auth), file));
    }

    @PostMapping("/{id}/confirm")
    public Result<Map<String, Object>> confirm(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(service.confirm(userId(auth), id, body));
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list(Authentication auth) {
        return Result.ok(service.list(userId(auth)));
    }
}

