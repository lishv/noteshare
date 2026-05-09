package com.note.controller.admin;

import com.note.dto.AdminChangePasswordDTO;
import com.note.dto.AdminLoginDTO;
import com.note.service.AdminAuthService;
import com.note.vo.AdminLoginVO;
import com.note.vo.AdminProfileVO;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "管理端-认证管理", description = "管理员登录登出相关接口")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员登录获取JWT令牌")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        AdminLoginVO result = adminAuthService.login(dto, ipAddress);
        return Result.success("登录成功", result);
    }

    @PostMapping("/logout")
    @Operation(summary = "管理员登出", description = "管理员退出登录")
    public Result<Void> logout() {
        return Result.success("退出登录成功");
    }

    @GetMapping("/profile")
    @Operation(summary = "获取当前管理员信息", description = "获取当前登录管理员信息")
    public Result<AdminProfileVO> getProfile(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        AdminProfileVO profile = adminAuthService.getProfile(adminId);
        return Result.success(profile);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "管理员修改密码")
    public Result<Void> changePassword(@Valid @RequestBody AdminChangePasswordDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String ipAddress = request.getRemoteAddr();
        adminAuthService.changePassword(adminId, dto, ipAddress);
        return Result.success("密码修改成功");
    }
}
