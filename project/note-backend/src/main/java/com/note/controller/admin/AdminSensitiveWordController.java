package com.note.controller.admin;

import com.note.dto.BatchDeleteDTO;
import com.note.dto.BatchStatusDTO;
import com.note.dto.SensitiveWordBatchDTO;
import com.note.dto.SensitiveWordDTO;
import com.note.entity.SensitiveWord;
import com.note.service.AdminSensitiveWordService;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/words")
@Tag(name = "管理端-敏感词管理", description = "管理端敏感词管理接口")
public class AdminSensitiveWordController {

    private final AdminSensitiveWordService adminSensitiveWordService;

    public AdminSensitiveWordController(AdminSensitiveWordService adminSensitiveWordService) {
        this.adminSensitiveWordService = adminSensitiveWordService;
    }

    @GetMapping
    @Operation(summary = "敏感词列表", description = "分页获取敏感词列表")
    public Result<PageResult<SensitiveWord>> listWords(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "类型") @RequestParam(required = false) Integer type,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(adminSensitiveWordService.listWords(page, size, keyword, type, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "敏感词详情", description = "获取敏感词详情")
    public Result<SensitiveWord> getWord(@PathVariable Long id) {
        return Result.success(adminSensitiveWordService.getWord(id));
    }

    @PostMapping
    @Operation(summary = "新增敏感词", description = "新增敏感词")
    public Result<Void> createWord(@Valid @RequestBody SensitiveWordDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminSensitiveWordService.createWord(dto, adminId, adminUsername, ipAddress);
        return Result.success("新增成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑敏感词", description = "编辑敏感词")
    public Result<Void> updateWord(@PathVariable Long id, @RequestBody SensitiveWordDTO dto,
                                    HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminSensitiveWordService.updateWord(id, dto, adminId, adminUsername, ipAddress);
        return Result.success("编辑成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除敏感词", description = "删除敏感词")
    public Result<Void> deleteWord(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminSensitiveWordService.deleteWord(id, adminId, adminUsername, ipAddress);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "变更敏感词状态", description = "启用或禁用敏感词")
    public Result<Void> toggleWordStatus(@PathVariable Long id, @RequestParam Integer status,
                                          HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminSensitiveWordService.toggleWordStatus(id, status, adminId, adminUsername, ipAddress);
        return Result.success("操作成功");
    }

    @PostMapping("/batch-import")
    @Operation(summary = "批量导入敏感词", description = "批量导入敏感词")
    public Result<Void> batchImport(@Valid @RequestBody SensitiveWordBatchDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminSensitiveWordService.batchImport(dto, adminId, adminUsername, ipAddress);
        return Result.success("批量导入成功");
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除敏感词", description = "批量删除敏感词")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminSensitiveWordService.batchDelete(dto.getIds(), adminId, adminUsername, ipAddress);
        return Result.success("批量删除成功");
    }

    @PutMapping("/batch/status")
    @Operation(summary = "批量变更敏感词状态", description = "批量启用或禁用敏感词")
    public Result<Void> batchToggleStatus(@Valid @RequestBody BatchStatusDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminSensitiveWordService.batchToggleStatus(dto.getIds(), dto.getStatus(), adminId, adminUsername, ipAddress);
        return Result.success("批量操作成功");
    }
}
