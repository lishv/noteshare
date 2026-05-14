package com.note.controller.admin;

import com.note.dto.AdminCreateDTO;
import com.note.dto.AdminEditDTO;
import com.note.entity.Admin;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.AdminMapper;
import com.note.service.AdminManageService;
import com.note.vo.AdminManageVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/admins")
@Tag(name = "管理端-管理员管理", description = "超级管理员管理普通管理员")
public class AdminManageController {

    private final AdminManageService adminManageService;
    private final AdminMapper adminMapper;

    public AdminManageController(AdminManageService adminManageService, AdminMapper adminMapper) {
        this.adminManageService = adminManageService;
        this.adminMapper = adminMapper;
    }

    private void checkSuperAdmin(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null || admin.getRole() != 1) {
            throw new BusinessException(ErrorCode.ADMIN_SUPER_REQUIRED);
        }
    }

    @GetMapping
    @Operation(summary = "管理员列表", description = "获取管理员列表（超管专用）")
    public Result<PageResult<AdminManageVO>> listAdmins(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        checkSuperAdmin(request);
        return Result.success(adminManageService.listAdmins(page, size, keyword));
    }

    @PostMapping
    @Operation(summary = "创建管理员", description = "创建新管理员（超管专用）")
    public Result<Void> createAdmin(@Valid @RequestBody AdminCreateDTO dto, HttpServletRequest request) {
        checkSuperAdmin(request);
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminManageService.createAdmin(dto, adminId, adminUsername, ipAddress);
        return Result.success("创建成功");
    }

    @PutMapping("/{adminId}")
    @Operation(summary = "编辑管理员", description = "编辑管理员信息（超管专用）")
    public Result<Void> editAdmin(@PathVariable Long adminId, @RequestBody AdminEditDTO dto,
                                   HttpServletRequest request) {
        checkSuperAdmin(request);
        Long operatorId = (Long) request.getAttribute("adminId");
        String operatorUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminManageService.editAdmin(adminId, dto, operatorId, operatorUsername, ipAddress);
        return Result.success("编辑成功");
    }

    @PutMapping("/{adminId}/status")
    @Operation(summary = "变更管理员状态", description = "启用或禁用管理员（超管专用）")
    public Result<Void> toggleAdminStatus(@PathVariable Long adminId, @RequestParam Integer status,
                                           HttpServletRequest request) {
        checkSuperAdmin(request);
        Long operatorId = (Long) request.getAttribute("adminId");
        String operatorUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminManageService.toggleAdminStatus(adminId, status, operatorId, operatorUsername, ipAddress);
        return Result.success("操作成功");
    }

    @PutMapping("/{adminId}/reset-password")
    @Operation(summary = "重置管理员密码", description = "重置管理员密码（超管专用）")
    public Result<Void> resetAdminPassword(@PathVariable Long adminId, HttpServletRequest request) {
        checkSuperAdmin(request);
        Long operatorId = (Long) request.getAttribute("adminId");
        String operatorUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminManageService.resetAdminPassword(adminId, operatorId, operatorUsername, ipAddress);
        return Result.success("密码已重置为默认密码");
    }

    @DeleteMapping("/{adminId}")
    @Operation(summary = "删除管理员", description = "删除管理员（超管专用）")
    public Result<Void> deleteAdmin(@PathVariable Long adminId, HttpServletRequest request) {
        checkSuperAdmin(request);
        Long operatorId = (Long) request.getAttribute("adminId");
        String operatorUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminManageService.deleteAdmin(adminId, operatorId, operatorUsername, ipAddress);
        return Result.success("删除成功");
    }
}
