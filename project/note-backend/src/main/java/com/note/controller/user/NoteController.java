package com.note.controller.user;

import com.note.dto.NoteDTO;
import com.note.service.NoteService;
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
@RequestMapping("/user/notes")
@Tag(name = "笔记管理", description = "笔记的增删改查相关接口")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @Operation(summary = "创建笔记", description = "创建新的笔记内容")
    public Result<NoteVO> createNote(@Valid @RequestBody NoteDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        NoteVO result = noteService.createNote(userId, dto);
        return Result.success("笔记创建成功", result);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户笔记列表", description = "分页获取指定用户的笔记列表")
    public Result<PageResult<NoteVO>> getUserNotes(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size) {
        PageResult<NoteVO> result = noteService.getUserNotes(userId, page, size);
        return Result.success(result);
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "获取笔记详情", description = "根据ID获取笔记详细信息")
    public Result<NoteVO> getNote(@Parameter(description = "笔记ID") @PathVariable Long noteId) {
        NoteVO result = noteService.getNoteById(noteId);
        return Result.success(result);
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "更新指定笔记的内容")
    public Result<NoteVO> updateNote(
            @Parameter(description = "笔记ID") @PathVariable Long noteId,
            @Valid @RequestBody NoteDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        NoteVO result = noteService.updateNote(noteId, userId, dto);
        return Result.success("笔记更新成功", result);
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "删除笔记", description = "删除指定的笔记")
    public Result<Void> deleteNote(@Parameter(description = "笔记ID") @PathVariable Long noteId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        noteService.deleteNote(noteId, userId);
        return Result.success("笔记删除成功", null);
    }

    @PostMapping("/{noteId}/view")
    @Operation(summary = "增加浏览量", description = "增加指定笔记的浏览量计数")
    public Result<Void> incrementViewCount(@Parameter(description = "笔记ID") @PathVariable Long noteId) {
        noteService.incrementViewCount(noteId);
        return Result.success(null);
    }
}
