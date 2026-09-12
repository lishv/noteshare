package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.Comment;
import com.note.vo.AdminCommentVO;
import com.note.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    List<CommentVO> selectCommentsByNoteId(@Param("noteId") Long noteId);

    @Select("SELECT c.id, c.note_id, c.user_id, u.username, u.avatar, c.parent_id, " +
            "c.reply_to_user_id, ru.username AS reply_to_username, c.reply_to_comment_id, " +
            "c.content, c.created_at, c.updated_at " +
            "FROM t_comment c " +
            "LEFT JOIN t_user u ON c.user_id = u.id " +
            "LEFT JOIN t_user ru ON c.reply_to_user_id = ru.id " +
            "WHERE c.id = #{id} AND c.is_deleted = 0")
    CommentVO selectCommentVOById(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM t_comment WHERE note_id = #{noteId} AND is_deleted = 0")
    Long countByNoteId(@Param("noteId") Long noteId);

    @Update("UPDATE t_comment SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicalDeleteComment(@Param("id") Long id);

    /**
     * 管理端分页查询全部评论，包含已删除评论。
     */
    IPage<AdminCommentVO> selectAdminCommentPage(
            Page<AdminCommentVO> page,
            @Param("keyword") String keyword,
            @Param("isDeleted") Integer isDeleted,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Update("UPDATE t_comment SET is_deleted = 1, updated_at = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int logicalDeleteByAdmin(@Param("id") Long id);

    @Update("UPDATE t_comment SET is_deleted = 0, updated_at = NOW() " +
            "WHERE id = #{id} AND is_deleted = 1")
    int restoreByAdmin(@Param("id") Long id);
}
