package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发送验证码请求参数")
public class SendCodeDTO {
    @Schema(description = "邮箱", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @Schema(description = "场景：register-注册，changePassword-修改密码", example = "register", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "场景不能为空")
    private String scene;
}
