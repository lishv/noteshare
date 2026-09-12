package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.OperationLog;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.CommentMapper;
import com.note.mapper.OperationLogMapper;
import com.note.vo.AdminCommentVO;
import com.note.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminCommentService {

    private final CommentMapper commentMapper;
    private final OperationLogMapper operationLogMapper;

    public AdminCommentService(CommentMapper commentMapper, OperationLogMapper operationLogMapper) {
        this.commentMapper = commentMapper;
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 分页查询全部评论，管理员可以按内容、作者、笔记、状态和时间筛选。
     */
    public PageResult<AdminCommentVO> listComments(Integer page, Integer size,
                                                    String keyword, Integer isDeleted,
                                                    String startTime, String endTime) {
        int currentPage = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;

        Page<AdminCommentVO> commentPage = new Page<>(currentPage, pageSize);
        IPage<AdminCommentVO> result = commentMapper.selectAdminCommentPage(
                commentPage, keyword, isDeleted, startTime, endTime);
        return PageResult.of(
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize(),
                result.getRecords());
    }

    @Transactional
    public void deleteComment(Long commentId, Long adminId, String adminUsername, String ipAddress) {
        if (commentId == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        int updated = commentMapper.logicalDeleteByAdmin(commentId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        logOperation(adminId, adminUsername, "COMMENT_MANAGE",
                "删除评论ID: " + commentId, "COMMENT", commentId, ipAddress);
    }

    @Transactional
    public void restoreComment(Long commentId, Long adminId, String adminUsername, String ipAddress) {
        if (commentId == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        int updated = commentMapper.restoreByAdmin(commentId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        logOperation(adminId, adminUsername, "COMMENT_MANAGE",
                "恢复评论ID: " + commentId, "COMMENT", commentId, ipAddress);
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
