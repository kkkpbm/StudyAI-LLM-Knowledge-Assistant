package com.ka.module.document.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

/** 学习资料导入、解析、确认入库的工作流服务。 */
public interface DocumentWorkflowService {
    Map<String, Object> parse(Long userId, MultipartFile file);
    Map<String, Object> confirm(Long userId, Long workflowId, Map<String, Object> body);
    List<Map<String, Object>> list(Long userId);
}

