package com.ka.module.learning.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("plan_items")
public class PlanItem {
    @TableId(type = IdType.AUTO)
    /** 计划项主键 ID */
    private Long id;
    /** 所属学习计划 ID */
    private Long planId;
    /** 计划项标题 */
    private String title;
    /** 计划项详细说明 */
    private String description;
    /** 关联笔记 ID，可为空 */
    private Long noteId;
    /** 计划项排序序号 */
    private Integer orderNum;
    /** 预计完成时长，单位为分钟 */
    private Integer estimatedMinutes;
    /** 是否已经完成 */
    private Boolean completed;
    /** 实际完成时间 */
    private LocalDateTime completedAt;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
}

