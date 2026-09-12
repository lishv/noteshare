package com.note.controller.user;

import com.note.dto.LoginDTO;
import com.note.dto.RegisterDTO;
import com.note.dto.ResetPasswordDTO;
import com.note.dto.SendCodeDTO;
import com.note.service.AuthService;
import com.note.util.JwtUtil;
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
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送验证码", description = "向邮箱发送注册、修改密码或重置密码验证码")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        authService.sendVerificationCode(dto);
        return Result.success("验证码发送成功");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "使用邮箱验证码重置用户密码")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success("密码重置成功");
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

    @GetMapping("/token-status")
    @Operation(summary = "查询Token状态", description = "返回当前Token剩余有效时间(秒)")
    public Result<Long> tokenStatus(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return Result.success(0L);
        }
        return Result.success(jwtUtil.getTokenRemainingSeconds(token));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
