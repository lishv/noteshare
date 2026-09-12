package com.note.controller.admin;

import com.note.service.AdminNotificationService;
import com.note.vo.AdminNotificationVO;
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
@RequestMapping("/admin/notifications")
@Tag(name = "管理端-用户消息", description = "管理端用户通知收集、审查与恢复接口")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    public AdminNotificationController(AdminNotificationService adminNotificationService) {
        this.adminNotificationService = adminNotificationService;
    }

    @GetMapping
    @Operation(summary = "用户消息列表", description = "分页查询全体用户通知，支持多条件筛选")
    public Result<PageResult<AdminNotificationVO>> listNotifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "搜索标题、内容、用户或关联对象") @RequestParam(required = false) String keyword,
            @Parameter(description = "接收用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "通知类型: COMMENT/FOLLOW/LIKE/SYSTEM") @RequestParam(required = false) String type,
            @Parameter(description = "已读状态: 0-未读, 1-已读") @RequestParam(required = false) Integer isRead,
            @Parameter(description = "移除状态: 0-正常, 1-已移除") @RequestParam(required = false) Integer isDeleted,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        return Result.success(adminNotificationService.listNotifications(
                page, size, keyword, userId, type, isRead, isDeleted, startTime, endTime));
    }

    @PutMapping("/{notificationId}/delete")
    @Operation(summary = "移除用户消息", description = "管理员软删除一条用户通知")
    public Result<Void> deleteNotification(@PathVariable Long notificationId,
                                           HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        adminNotificationService.deleteNotification(
                notificationId, adminId, adminUsername, request.getRemoteAddr());
        return Result.success("消息已移除");
    }

    @PutMapping("/{notificationId}/restore")
    @Operation(summary = "恢复用户消息", description = "恢复一条已移除的用户通知")
    public Result<Void> restoreNotification(@PathVariable Long notificationId,
                                            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        adminNotificationService.restoreNotification(
                notificationId, adminId, adminUsername, request.getRemoteAddr());
        return Result.success("消息已恢复");
    }
}
