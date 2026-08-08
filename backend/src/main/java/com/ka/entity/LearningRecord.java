package com.ka.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("learning_records")
public class LearningRecord {
    @TableId(type = IdType.AUTO)
    /** 学习记录主键 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 关联笔记 ID，可为空 */
    private Long noteId;
    @TableField("duration_minutes")
    /** 本次学习时长，单位为分钟 */
    private Integer durationMinutes;
    @TableField(exist = false)
    /** 理解程度（非数据库字段） */
    private Integer comprehensionLevel;
    @TableField(exist = false)
    /** 学习备注（非数据库字段） */
    private String notes;
    @TableField(value = "date", fill = FieldFill.INSERT)
    /** 学习记录日期 */
    private LocalDateTime recordedAt;
}
