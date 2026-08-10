package com.ka.module.user.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    /** 用户主键 ID */
    private Long id;
    /** 用户登录名 */
    private String username;
    /** BCrypt 加密后的登录密码 */
    private String password;
    /** 用户邮箱 */
    private String email;
    /** 用户头像访问地址 */
    private String avatar;
    /** 用户角色，如 USER 或 ADMIN */
    private String role;
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

