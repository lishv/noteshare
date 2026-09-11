package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改个性签名请求")
public class UpdateBioDTO {
    @Schema(description = "个性签名 (最多30字)", example = "努力学习，天天向上")
    @Size(max = 30, message = "个性签名不能超过30个字符")
    private String bio;
}
