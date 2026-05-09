package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码请求参数")
public class ChangePasswordDTO {
    @Schema(description = "旧密码（通过旧密码修改时必填）")
    private String oldPassword;

    @Schema(description = "新密码", example = "newpass123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String newPassword;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String email;

    @Schema(description = "邮箱验证码", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String verificationCode;
}
