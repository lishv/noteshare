package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分布统计数据")
public class DistributionVO {
    @Schema(description = "范围/区间")
    private String range;

    @Schema(description = "数量")
    private Long count;
}
