package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "笔记草稿请求参数")
public class NoteDraftDTO {

    @Schema(description = "笔记ID (若为新建草稿则为空或不传)", example = "1")
    private Long noteId;

    @Schema(description = "笔记标题", example = "Redis缓存与草稿设计")
    private String title;

    @Schema(description = "笔记正文内容 (Markdown格式)")
    private String content;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "是否公开: 1-公开, 0-私密", example = "1")
    private Integer isPublic;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;
}
