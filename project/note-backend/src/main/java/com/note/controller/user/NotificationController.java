package com.note.controller.user;

import com.note.enums.ErrorCode;
import com.note.service.NotificationService;
import com.note.vo.NotificationVO;
import com.note.vo.PageResult;
import com.note.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/notifications")
@Tag(name = "站内通知", description = "用户消息通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "获取通知列表", description = "分页获取当前登录用户的通知列表，支持按类型筛选")
    public Result<PageResult<NotificationVO>> getNotifications(
            @Parameter(description = "通知类型: COMMENT, FOLLOW, LIKE, SYSTEM (为空则查全部)") @RequestParam(required = false) String type,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "15") Integer size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        PageResult<NotificationVO> result = notificationService.getNotifications(userId, type, page, size);
        return Result.success(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数", description = "获取当前登录用户的未读通知数量")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(0L);
        }
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读", description = "将指定的通知标记为已读")
    public Result<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        notificationService.markAsRead(id, userId);
        return Result.success("已标记为已读", null);
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记为已读", description = "将当前登录用户的所有未读通知标记为已读")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        notificationService.markAllAsRead(userId);
        return Result.success("全部已标记为已读", null);
    }
}
