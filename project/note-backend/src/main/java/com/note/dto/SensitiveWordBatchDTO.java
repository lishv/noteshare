package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量导入敏感词请求参数")
public class SensitiveWordBatchDTO {
    @Schema(description = "敏感词列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "敏感词列表不能为空")
    private List<String> words;

    @Schema(description = "类型: 1-政治, 2-色情, 3-暴力, 4-广告")
    private Integer type;
}
