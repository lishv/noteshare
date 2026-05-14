package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "笔记信息响应数据")
public class NoteVO {
    @Schema(description = "笔记ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "testuser")
    private String username;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "笔记标题", example = "Java基础笔记")
    private String title;

    @Schema(description = "链接或位置", example = "https://example.com/note")
    private String locationOrUrl;

    @Schema(description = "浏览量", example = "10")
    private Long viewCount;

    @Schema(description = "是否已删除: 0-正常, 1-已删除")
    private Integer isDeleted;

    @Schema(description = "是否置顶: 0-否, 1-是")
    private Integer isPinned;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
