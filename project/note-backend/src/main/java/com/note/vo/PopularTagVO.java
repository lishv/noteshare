package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热门标签统计VO")
public class PopularTagVO {

    @Schema(description = "标签名称", example = "SpringBoot 3")
    private String name;

    @Schema(description = "关联的公开有效笔记数", example = "15")
    private Long noteCount;
}
