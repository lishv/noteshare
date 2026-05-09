package com.note.controller.admin;

import com.note.dto.BatchDeleteDTO;
import com.note.service.AdminNoteService;
import com.note.vo.NoteVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notes")
@Tag(name = "管理端-笔记管理", description = "管理端笔记管理接口")
public class AdminNoteController {

    private final AdminNoteService adminNoteService;

    public AdminNoteController(AdminNoteService adminNoteService) {
        this.adminNoteService = adminNoteService;
    }

    @GetMapping
    @Operation(summary = "笔记列表", description = "分页获取笔记列表")
    public Result<PageResult<NoteVO>> listNotes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "删除状态: 0-正常, 1-已删除") @RequestParam(required = false) Integer isDeleted) {
        return Result.success(adminNoteService.listNotes(page, size, keyword, isDeleted));
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "笔记详情", description = "获取笔记详细信息")
    public Result<NoteVO> getNoteDetail(@PathVariable Long noteId) {
        return Result.success(adminNoteService.getNoteDetail(noteId));
    }

    @PutMapping("/{noteId}/delete")
    @Operation(summary = "逻辑删除笔记", description = "将笔记标记为已删除")
    public Result<Void> logicalDeleteNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminNoteService.logicalDeleteNote(noteId, adminId, adminUsername, ipAddress);
        return Result.success("删除成功");
    }

    @PutMapping("/{noteId}/restore")
    @Operation(summary = "恢复笔记", description = "恢复已删除的笔记")
    public Result<Void> restoreNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminNoteService.restoreNote(noteId, adminId, adminUsername, ipAddress);
        return Result.success("恢复成功");
    }

    @PutMapping("/batch/delete")
    @Operation(summary = "批量删除笔记", description = "批量逻辑删除笔记")
    public Result<Void> batchDeleteNotes(@Valid @RequestBody BatchDeleteDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminNoteService.batchDeleteNotes(dto.getIds(), adminId, adminUsername, ipAddress);
        return Result.success("批量删除成功");
    }

    @PutMapping("/batch/restore")
    @Operation(summary = "批量恢复笔记", description = "批量恢复已删除的笔记")
    public Result<Void> batchRestoreNotes(@Valid @RequestBody BatchDeleteDTO dto, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminNoteService.batchRestoreNotes(dto.getIds(), adminId, adminUsername, ipAddress);
        return Result.success("批量恢复成功");
    }
}
