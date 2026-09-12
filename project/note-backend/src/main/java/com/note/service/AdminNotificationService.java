package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.OperationLog;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.NotificationMapper;
import com.note.mapper.OperationLogMapper;
import com.note.vo.AdminNotificationVO;
import com.note.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminNotificationService {

    private final NotificationMapper notificationMapper;
    private final OperationLogMapper operationLogMapper;

    public AdminNotificationService(NotificationMapper notificationMapper,
                                    OperationLogMapper operationLogMapper) {
        this.notificationMapper = notificationMapper;
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 分页查询全体用户通知，包含已从用户端移除的消息，供管理员收集和审查。
     */
    public PageResult<AdminNotificationVO> listNotifications(
            Integer page, Integer size, String keyword, Long userId, String type,
            Integer isRead, Integer isDeleted, String startTime, String endTime) {
        int currentPage = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;

        Page<AdminNotificationVO> notificationPage = new Page<>(currentPage, pageSize);
        IPage<AdminNotificationVO> result = notificationMapper.selectAdminNotificationPage(
                notificationPage, keyword, userId, type, isRead, isDeleted, startTime, endTime);
        return PageResult.of(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                result.getRecords());
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long adminId,
                                   String adminUsername, String ipAddress) {
        if (notificationId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        int updated = notificationMapper.logicalDeleteByAdmin(notificationId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在或已经移除");
        }
        logOperation(adminId, adminUsername, "NOTIFICATION_MANAGE",
                "移除通知ID: " + notificationId, "NOTIFICATION", notificationId, ipAddress);
    }

    @Transactional
    public void restoreNotification(Long notificationId, Long adminId,
                                    String adminUsername, String ipAddress) {
        if (notificationId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        int updated = notificationMapper.restoreByAdmin(notificationId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在或未被移除");
        }
        logOperation(adminId, adminUsername, "NOTIFICATION_MANAGE",
                "恢复通知ID: " + notificationId, "NOTIFICATION", notificationId, ipAddress);
    }

    private void logOperation(Long adminId, String adminUsername, String operationType,
                              String description, String targetType, Long targetId, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setOperationType(operationType);
        log.setDescription(description);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
