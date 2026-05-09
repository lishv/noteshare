package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "趋势数据")
public class TrendVO {
    @Schema(description = "日期")
    private String date;

    @Schema(description = "数量")
    private Long count;
}
