package com.ka.module.knowledge.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notes")
public class Note {
    @TableId(type = IdType.AUTO)
    /** 笔记主键 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 所属分类 ID，可为空 */
    private Long categoryId;
    /** 笔记标题 */
    private String title;
    /** Markdown 格式的笔记正文 */
    private String contentMd;
    @TableField(exist = false)
    /** 纯文本正文，用于展示或检索（非数据库字段） */
    private String contentPlain;
    @TableField(exist = false)
    /** 笔记摘要（非数据库字段） */
    private String summary;
    /** 难度等级，如 beginner、intermediate、advanced */
    private String difficultyLevel;
    @TableField(exist = false)
    /** 笔记状态（非数据库字段） */
    private Integer status;
    @TableField(exist = false)
    /** 笔记浏览次数（非数据库字段） */
    private Integer viewCount;
    @TableLogic
    /** 逻辑删除标记 */
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}

