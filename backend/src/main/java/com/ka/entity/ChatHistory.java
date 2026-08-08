package com.ka.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_history")
public class ChatHistory {
    @TableId(type = IdType.AUTO)
    /** 聊天记录主键 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 消息角色，如 user 或 assistant */
    private String role;
    /** 消息正文 */
    private String content;
    /** 关联笔记 ID，可为空 */
    private Long noteId;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
}
