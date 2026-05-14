package com.note.controller.admin;

import com.note.dto.BatchDeleteDTO;
import com.note.dto.EditLinkDTO;
import com.note.service.AdminLinkService;
import com.note.vo.MainLinkVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/links")
@Tag(name = "管理端-链接管理", description = "管理端链接管理接口")
public class AdminLinkController {

    private final AdminLinkService adminLinkService;

    public AdminLinkController(AdminLinkService adminLinkService) {
        this.adminLinkService = adminLinkService;
    }

    @GetMapping
    @Operation(summary = "链接列表", description = "分页获取链接列表")
    public Result<PageResult<MainLinkVO>> listLinks(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return Result.success(adminLinkService.listLinks(page, size, keyword));
    }

    @PutMapping("/{linkId}")
    @Operation(summary = "编辑链接", description = "编辑指定链接")
    public Result<Void> editLink(@PathVariable Long linkId, @Valid @RequestBody EditLinkDTO dto,
                                  HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminLinkService.editLink(linkId, dto.getMainUrl(), adminId, adminUsername, ipAddress);
        return Result.success("编辑成功");
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "删除链接", description = "删除指定链接")
    public Result<Void> deleteLink(@PathVariable Long linkId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminLinkService.deleteLink(linkId, adminId, adminUsername, ipAddress);
        return Result.success("删除成功");
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除链接", description = "批量删除链接")
    public Result<Void> batchDeleteLinks(@Valid @RequestBody BatchDeleteDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminLinkService.batchDeleteLinks(dto.getIds(), adminId, adminUsername, ipAddress);
        return Result.success("批量删除成功");
    }
}
