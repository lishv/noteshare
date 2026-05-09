package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量删除请求参数")
public class BatchDeleteDTO {
    @Schema(description = "ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;
}
