package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "笔记请求参数")
public class NoteDTO {
    @Schema(description = "笔记名称", example = "Java基础笔记", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "笔记名称不能为空")
    @Size(max = 100, message = "笔记名称不能超过100个字符")
    private String title;

    @Schema(description = "笔记类型: 1-链接笔记, 2-创作笔记", example = "1")
    private Integer noteType;

    @Schema(description = "链接或位置 (链接笔记必填)", example = "https://example.com/note")
    private String locationOrUrl;

    @Schema(description = "笔记正文内容 (Markdown格式, 创作笔记必填)")
    private String content;

    @Schema(description = "笔记摘要 (可选，不填则根据内容自动截取)")
    private String summary;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "是否公开: 1-公开, 0-私密", example = "1")
    private Integer isPublic;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "标签名称列表")
    private List<String> tagNames;
}
