package com.note.service;

import com.note.dto.CommentDTO;
import com.note.entity.Comment;
import com.note.entity.NoteItem;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.CommentMapper;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.UserMapper;
import com.note.vo.CommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final NoteItemMapper noteItemMapper;
    private final UserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;

    public CommentService(CommentMapper commentMapper,
                          NoteItemMapper noteItemMapper,
                          UserMapper userMapper,
                          SensitiveWordService sensitiveWordService) {
        this.commentMapper = commentMapper;
        this.noteItemMapper = noteItemMapper;
        this.userMapper = userMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    /**
     * 获取指定笔记的全部有效评论列表
     */
    public List<CommentVO> listComments(Long noteId, Long currentUserId) {
        // 校验笔记是否存在
        NoteItem note = noteItemMapper.selectById(noteId);
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        List<CommentVO> list = commentMapper.selectCommentsByNoteId(noteId);
        for (CommentVO vo : list) {
            vo.setIsOwner(currentUserId != null && currentUserId.equals(vo.getUserId()));
        }
        return list;
    }

    /**
     * 发表或回复评论
     */
    @Transactional
    public CommentVO createComment(Long userId, Long noteId, CommentDTO dto) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评论内容不能为空");
        }

        // 校验笔记是否存在
        NoteItem note = noteItemMapper.selectById(noteId);
        if (note == null || note.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        }

        // 敏感词校验
        if (sensitiveWordService.containsSensitiveWord(dto.getContent())) {
            throw new BusinessException(ErrorCode.COMMENT_CONTAINS_SENSITIVE_WORD);
        }

        Comment comment = new Comment();
        comment.setNoteId(noteId);
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId());
        comment.setReplyToUserId(dto.getReplyToUserId());
        comment.setReplyToCommentId(dto.getReplyToCommentId());
        comment.setContent(dto.getContent().trim());
        comment.setIsDeleted(0);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        commentMapper.insert(comment);

        // 更新用户最后活跃时间
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastActiveAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        CommentVO vo = commentMapper.selectCommentVOById(comment.getId());
        if (vo != null) {
            vo.setIsOwner(true);
        }
        return vo;
    }

    /**
     * 删除评论 (评论作者本人、笔记作者均有权删除)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        NoteItem note = noteItemMapper.selectById(comment.getNoteId());
        boolean isCommentOwner = comment.getUserId().equals(userId);
        boolean isNoteAuthor = note != null && note.getUserId().equals(userId);

        if (!isCommentOwner && !isNoteAuthor) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此评论");
        }

        commentMapper.logicalDeleteComment(commentId);
    }
}
