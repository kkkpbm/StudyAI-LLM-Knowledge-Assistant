package com.ka.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("learning_plans")
public class LearningPlan {
    @TableId(type = IdType.AUTO)
    /** 学习计划主键 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 学习计划标题 */
    private String title;
    /** 学习计划描述 */
    private String description;
    /** 学习目标 */
    private String goal;
    /** 计划开始日期 */
    private LocalDate startDate;
    /** 计划结束日期 */
    private LocalDate endDate;
    /** 计划状态，通常 1 表示进行中 */
    private Integer status;
    /** 是否由 AI 自动生成 */
    private Boolean aiGenerated;
    @TableLogic
    /** 逻辑删除标记 */
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
}
