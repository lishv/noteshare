package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "修改标签请求参数")
public class UpdateTagsDTO {
    @Schema(description = "标签名称列表")
    private List<String> tagNames;
}
