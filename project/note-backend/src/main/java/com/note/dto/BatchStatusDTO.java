package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量状态变更请求参数")
public class BatchStatusDTO {
    @Schema(description = "ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
