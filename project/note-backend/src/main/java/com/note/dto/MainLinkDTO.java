package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "总链接请求参数")
public class MainLinkDTO {
    @Schema(description = "总链接URL", example = "https://notion.so/username", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "总链接URL不能为空")
    private String mainUrl;
}
