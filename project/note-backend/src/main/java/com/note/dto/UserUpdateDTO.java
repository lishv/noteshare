package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "管理员编辑修改用户请求参数")
public class UserUpdateDTO {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度为2-50个字符")
    private String username;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "个性签名 (最多30字)")
    @Size(max = 30, message = "个性签名不能超过30个字符")
    private String bio;

    @Schema(description = "用户状态: 0-禁用, 1-正常, 2-锁定")
    private Integer status;

    @Schema(description = "新密码 (留空则不修改密码)")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String password;
}
