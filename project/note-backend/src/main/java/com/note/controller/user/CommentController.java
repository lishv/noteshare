package com.note.controller.user;

import com.note.dto.CommentDTO;
import com.note.service.CommentService;
import com.note.vo.CommentVO;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/notes/{noteId}/comments")
@Tag(name = "评论管理", description = "笔记评论与回复相关接口")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "获取笔记评论列表", description = "获取指定笔记下的所有有效评论与回复")
    public Result<List<CommentVO>> listComments(
            @Parameter(description = "笔记ID") @PathVariable Long noteId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<CommentVO> list = commentService.listComments(noteId, userId);
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "发表或回复评论", description = "在指定笔记下发表评论或回复他人评论")
    public Result<CommentVO> createComment(
            @Parameter(description = "笔记ID") @PathVariable Long noteId,
            @Valid @RequestBody CommentDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        CommentVO comment = commentService.createComment(userId, noteId, dto);
        return Result.success("评论发表成功", comment);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定评论（仅评论作者或笔记作者可删除）")
    public Result<Void> deleteComment(
            @Parameter(description = "笔记ID") @PathVariable Long noteId,
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.deleteComment(commentId, userId);
        return Result.success("评论删除成功", null);
    }
}
