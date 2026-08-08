package com.ka.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tags")
public class Tag {
    @TableId(type = IdType.AUTO)
    /** 标签主键 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 标签名称 */
    private String name;
    @TableField(exist = false)
    /** 标签展示颜色（非数据库字段） */
    private String color;
    @TableLogic
    /** 逻辑删除标记 */
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
}
