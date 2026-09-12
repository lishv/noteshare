package com.note.controller.admin;

import com.note.service.AdminCommentService;
import com.note.vo.AdminCommentVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/comments")
@Tag(name = "管理端-评论管理", description = "管理端评论查询、删除与恢复接口")
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @GetMapping
    @Operation(summary = "评论列表", description = "分页查询全部评论，支持关键词、删除状态和时间筛选")
    public Result<PageResult<AdminCommentVO>> listComments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "搜索评论内容、作者或笔记标题") @RequestParam(required = false) String keyword,
            @Parameter(description = "删除状态: 0-正常, 1-已删除") @RequestParam(required = false) Integer isDeleted,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        return Result.success(adminCommentService.listComments(
                page, size, keyword, isDeleted, startTime, endTime));
    }

    @PutMapping("/{commentId}/delete")
    @Operation(summary = "删除评论", description = "软删除一条评论")
    public Result<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        adminCommentService.deleteComment(
                commentId, adminId, adminUsername, request.getRemoteAddr());
        return Result.success("评论已删除");
    }

    @PutMapping("/{commentId}/restore")
    @Operation(summary = "恢复评论", description = "恢复一条已删除的评论")
    public Result<Void> restoreComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        adminCommentService.restoreComment(
                commentId, adminId, adminUsername, request.getRemoteAddr());
        return Result.success("评论已恢复");
    }
}
