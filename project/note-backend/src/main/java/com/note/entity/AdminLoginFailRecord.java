package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("t_admin_login_fail_record")
@Data
public class AdminLoginFailRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String ipAddress;

    private Integer failCount;

    private LocalDateTime lockUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
