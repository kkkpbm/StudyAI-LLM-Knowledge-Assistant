package com.ka.module.learning.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("review_reminders")
public class ReviewReminder {
    @TableId(type = IdType.AUTO)
    /** 复习提醒主键 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 需要复习的笔记 ID */
    private Long noteId;
    /** 下次复习日期 */
    private LocalDate nextReviewAt;
    /** 当前复习间隔，单位为天 */
    private Integer intervalDays;
    @TableField("repetitions")
    /** 连续成功复习次数 */
    private Integer repetitionCount;
    @TableField("ease_factor")
    /** SM-2 算法的难度因子 */
    private Double easeFactor;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
}

