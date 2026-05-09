package com.note.controller.user;

import com.note.dto.LoginDTO;
import com.note.dto.RegisterDTO;
import com.note.dto.SendCodeDTO;
import com.note.service.AuthService;
import com.note.service.VerificationCodeService;
import com.note.vo.LoginVO;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/auth")
@Tag(name = "认证管理", description = "用户注册、登录相关接口")
public class AuthController {

    private final AuthService authService;
    private final VerificationCodeService verificationCodeService;

    public AuthController(AuthService authService, VerificationCodeService verificationCodeService) {
        this.authService = authService;
        this.verificationCodeService = verificationCodeService;
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送验证码", description = "向邮箱发送验证码")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        verificationCodeService.sendCode(dto.getEmail(), dto.getScene());
        return Result.success("验证码发送成功");
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账号")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        LoginVO result = authService.register(dto);
        return Result.success("注册成功", result);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取JWT令牌")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        LoginVO result = authService.login(dto, ipAddress);
        return Result.success("登录成功", result);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出登录")
    public Result<Void> logout() {
        return Result.success("退出登录成功");
    }
}
