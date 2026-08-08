package com.ka.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("categories")
public class Category {
    @TableId(type = IdType.AUTO)
    /** 分类主键 ID */
    private Long id;
    /** 所属用户 ID */
    private Long userId;
    /** 分类名称 */
    private String name;
    @TableField(exist = false)
    /** 分类描述（非数据库字段） */
    private String description;
    /** 分类展示颜色 */
    private String color;
    @TableField(exist = false)
    /** 父分类 ID（非数据库字段） */
    private Long parentId;
    @TableField(exist = false)
    /** 分类排序值（非数据库字段） */
    private Integer sortOrder;
    @TableLogic
    /** 逻辑删除标记 */
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    /** 创建时间 */
    private LocalDateTime createdAt;
}
