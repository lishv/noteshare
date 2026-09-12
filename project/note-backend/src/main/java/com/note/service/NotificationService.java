package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.Notification;
import com.note.mapper.NotificationMapper;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.vo.NotificationVO;
import com.note.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 发送通知 (如果接收者与触发者为同一人，则不发送)
     */
    public void sendNotification(Long userId, Long senderId, String type, String title, String content, String targetType, Long targetId) {
        if (userId == null) {
            return;
        }
        // 自己给自己触发的动作不产生通知
        if (senderId != null && senderId.equals(userId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content != null ? content : "");
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setIsRead(0);
        notification.setIsDeleted(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    /**
     * 分页查询通知列表
     */
    public PageResult<NotificationVO> getNotifications(Long userId, String type, Integer page, Integer size) {
        int p = (page != null && page > 0) ? page : 1;
        int s = (size != null && size > 0) ? size : 15;
        Page<NotificationVO> pageParam = new Page<>(p, s);
        IPage<NotificationVO> result = notificationMapper.selectNotificationPage(pageParam, userId, type);
        return PageResult.of(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords());
    }

    /**
     * 移除当前用户的一条通知（软删除，保留管理员审查能力）
     */
    @Transactional
    public void removeNotification(Long id, Long userId) {
        if (id == null || userId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在或无权操作");
        }
        int updated = notificationMapper.logicalDeleteForUser(id, userId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在或无权操作");
        }
    }

    /**
     * 获取未读通知数量
     */
    public Long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Long count = notificationMapper.countUnread(userId);
        return count != null ? count : 0L;
    }

    /**
     * 标记单条通知已读
     */
    @Transactional
    public void markAsRead(Long id, Long userId) {
        if (userId == null || id == null) {
            return;
        }
        int updated = notificationMapper.markAsRead(id, userId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在或无权操作");
        }
    }

    /**
     * 一键将所有通知标记为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        if (userId == null) {
            return;
        }
        notificationMapper.markAllAsRead(userId);
    }
}
