package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "编辑链接请求参数")
public class EditLinkDTO {
    @Schema(description = "总链接URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "链接URL不能为空")
    private String mainUrl;
}
