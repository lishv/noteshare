package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "总链接响应数据")
public class MainLinkVO {
    @Schema(description = "总链接ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "总链接URL", example = "https://notion.so/username")
    private String mainUrl;

    @Schema(description = "浏览量", example = "5")
    private Long viewCount;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
