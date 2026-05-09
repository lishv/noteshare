package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "活跃用户响应数据")
public class ActiveUserVO {
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "testuser")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "状态: 0-禁用, 1-正常, 2-锁定")
    private Integer status;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveAt;

    @Schema(description = "是否有总链接", example = "true")
    private Boolean hasMainLink;

    @Schema(description = "笔记数量", example = "5")
    private Integer noteCount;

    @Schema(description = "注册时间")
    private LocalDateTime createdAt;
}
