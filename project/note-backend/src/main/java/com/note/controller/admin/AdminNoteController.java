package com.note.controller.admin;

import com.note.dto.BatchDeleteDTO;
import com.note.dto.NoteDTO;
import com.note.dto.UpdateTagsDTO;
import com.note.service.AdminNoteService;
import com.note.service.FavoriteService;
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
    private final FavoriteService favoriteService;

    public AdminNoteController(AdminNoteService adminNoteService, FavoriteService favoriteService) {
        this.adminNoteService = adminNoteService;
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @Operation(summary = "笔记列表", description = "分页获取笔记列表")
    public Result<PageResult<NoteVO>> listNotes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "笔记类型: 1-链接, 2-创作") @RequestParam(required = false) Integer noteType,
            @Parameter(description = "公开状态: 0-私密, 1-公开") @RequestParam(required = false) Integer isPublic,
            @Parameter(description = "删除状态: 0-正常, 1-已删除") @RequestParam(required = false) Integer isDeleted) {
        return Result.success(adminNoteService.listNotes(page, size, keyword, noteType, isPublic, isDeleted));
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "笔记详情", description = "获取笔记详细信息")
    public Result<NoteVO> getNoteDetail(@PathVariable Long noteId) {
        return Result.success(adminNoteService.getNoteDetail(noteId));
    }

    @PostMapping
    @Operation(summary = "插入笔记", description = "管理员为指定用户插入笔记")
    public Result<NoteVO> createNote(
            @RequestParam Long userId,
            @Valid @RequestBody NoteDTO dto,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        NoteVO vo = adminNoteService.createNote(userId, dto, adminId, adminUsername, ipAddress);
        return Result.success("插入成功", vo);
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

    @PutMapping("/{noteId}/pin")
    @Operation(summary = "置顶笔记", description = "置顶指定笔记")
    public Result<Void> pinNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminNoteService.pinNote(noteId, adminId, adminUsername, ipAddress);
        return Result.success("置顶成功");
    }

    @PutMapping("/{noteId}/unpin")
    @Operation(summary = "取消置顶笔记", description = "取消置顶指定笔记")
    public Result<Void> unpinNote(@PathVariable Long noteId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminNoteService.unpinNote(noteId, adminId, adminUsername, ipAddress);
        return Result.success("取消置顶成功");
    }

    @GetMapping("/{noteId}/favorites")
    @Operation(summary = "笔记收藏数", description = "获取指定笔记的收藏数")
    public Result<Long> getNoteFavoriteCount(@PathVariable Long noteId) {
        return Result.success(favoriteService.countByNoteId(noteId));
    }

    @PutMapping("/{noteId}/tags")
    @Operation(summary = "修改笔记标签", description = "管理员修改指定笔记的标签列表")
    public Result<Void> updateNoteTags(
            @PathVariable Long noteId,
            @RequestBody UpdateTagsDTO dto,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        String ipAddress = request.getRemoteAddr();
        adminNoteService.updateNoteTags(noteId, dto != null ? dto.getTagNames() : null, adminId, adminUsername, ipAddress);
        return Result.success("标签修改成功");
    }
}
