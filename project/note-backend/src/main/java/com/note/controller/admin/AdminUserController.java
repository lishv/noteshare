package com.note.controller.admin;

import com.note.dto.BatchStatusDTO;
import com.note.service.AdminUserService;
import com.note.vo.ActiveUserVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import com.note.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "管理端-用户管理", description = "管理端用户管理接口")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "用户列表", description = "分页获取用户列表")
    public Result<PageResult<ActiveUserVO>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选: 0-禁用, 1-正常, 2-锁定") @RequestParam(required = false) Integer status) {
        return Result.success(adminUserService.listUsers(page, size, keyword, status));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "用户详情", description = "获取用户详细信息")
    public Result<UserVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(adminUserService.getUserDetail(userId));
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "变更用户状态", description = "启用或禁用用户")
    public Result<Void> toggleUserStatus(@PathVariable Long userId,
                                         @RequestParam Integer status,
                                         HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminUserService.toggleUserStatus(userId, status, adminId, adminUsername, ipAddress);
        return Result.success("操作成功");
    }

    @PostMapping("/{userId}/unlock")
    @Operation(summary = "解锁用户", description = "解锁被锁定的用户")
    public Result<Void> unlockUser(@PathVariable Long userId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminUserService.unlockUser(userId, adminId, adminUsername, ipAddress);
        return Result.success("解锁成功");
    }

    @PutMapping("/{userId}/reset-password")
    @Operation(summary = "重置用户密码", description = "重置用户密码为默认密码")
    public Result<Void> resetUserPassword(@PathVariable Long userId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminUserService.resetUserPassword(userId, adminId, adminUsername, ipAddress);
        return Result.success("密码已重置为默认密码");
    }

    @PutMapping("/batch/status")
    @Operation(summary = "批量变更用户状态", description = "批量启用或禁用用户")
    public Result<Void> batchToggleStatus(@Valid @RequestBody BatchStatusDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminUserService.batchToggleStatus(dto.getIds(), dto.getStatus(), adminId, adminUsername, ipAddress);
        return Result.success("批量操作成功");
    }
}
