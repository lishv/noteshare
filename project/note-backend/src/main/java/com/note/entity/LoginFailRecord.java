package com.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_login_fail_record")
public class LoginFailRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String ipAddress;

    private Integer failCount = 1;

    private LocalDateTime lockUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
