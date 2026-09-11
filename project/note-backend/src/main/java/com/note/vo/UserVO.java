package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息响应数据")
public class UserVO {
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "testuser")
    private String username;

    @Schema(description = "邮箱", example = "test@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "个性签名 (最多30字)", example = "努力学习，天天向上")
    private String bio;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
