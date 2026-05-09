package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "编辑管理员请求参数")
public class AdminEditDTO {
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色: 1-超级管理员, 2-普通管理员")
    private Integer role;
}
