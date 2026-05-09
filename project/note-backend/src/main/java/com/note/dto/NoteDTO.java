package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "笔记请求参数")
public class NoteDTO {
    @Schema(description = "笔记名称", example = "Java基础笔记", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "笔记名称不能为空")
    @Size(max = 100, message = "笔记名称不能超过100个字符")
    private String title;

    @Schema(description = "链接或位置", example = "https://example.com/note", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "链接/位置不能为空")
    private String locationOrUrl;
}
