package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "敏感词请求参数")
public class SensitiveWordDTO {
    @Schema(description = "敏感词内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "敏感词不能为空")
    private String word;

    @Schema(description = "类型: 1-政治, 2-色情, 3-暴力, 4-广告")
    private Integer type;

    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;
}
