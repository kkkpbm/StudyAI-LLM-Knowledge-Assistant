package com.ka.module.knowledge.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_relations")
public class KnowledgeRelation {
    @TableId(type = IdType.AUTO)
    /** 知识关系主键 ID */
    private Long id;
    /** 关系来源笔记 ID */
    private Long noteId;
    /** 源概念名称 */
    private String source;
    /** 目标概念名称 */
    private String target;
    /** 关系类型，如 related、extends */
    private String relationType;
    /** 关系权重，取值范围 0 到 1 */
    private Double weight;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
}

