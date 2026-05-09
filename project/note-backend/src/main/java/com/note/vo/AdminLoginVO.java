package com.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员登录响应数据")
public class AdminLoginVO {
    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "是否需要修改密码")
    private Boolean mustChangePassword;

    @Schema(description = "管理员信息")
    private AdminProfileVO admin;
}
