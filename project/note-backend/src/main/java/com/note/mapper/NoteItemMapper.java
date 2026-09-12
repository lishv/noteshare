package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.NoteItem;
import com.note.vo.NoteVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NoteItemMapper extends BaseMapper<NoteItem> {

    @Update("UPDATE t_note_item SET view_count = view_count + 1 WHERE id = #{id} AND is_deleted = 0")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE t_note_item SET is_pinned = #{pinned}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updatePinned(@Param("id") Long id, @Param("pinned") Integer pinned);

    @Update("UPDATE t_note_item SET is_deleted = 1, deleted_at = #{deletedAt}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicalDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Update("UPDATE t_note_item SET is_deleted = 0, deleted_at = NULL, updated_at = NOW() WHERE id = #{id} AND is_deleted = 1")
    int restoreNote(@Param("id") Long id);

    @Select("SELECT n.id, n.user_id, u.username, u.avatar, n.title, n.note_type, n.location_or_url, n.content, n.summary, n.cover_image, n.view_count, n.is_pinned as isPinned, n.is_public as isPublic, n.created_at, n.updated_at, " +
            "COALESCE((SELECT COUNT(1) FROM t_note_like l WHERE l.note_id = n.id), 0) as likeCount, " +
            "COALESCE((SELECT COUNT(1) FROM t_note_favorite f WHERE f.note_id = n.id), 0) as favoriteCount, " +
            "COALESCE((SELECT COUNT(1) FROM t_comment c WHERE c.note_id = n.id AND c.is_deleted = 0), 0) as commentCount " +
            "FROM t_note_item n LEFT JOIN t_user u ON n.user_id = u.id " +
            "WHERE n.id = #{id} AND n.is_deleted = 0")
    NoteVO selectNoteWithUserById(@Param("id") Long id);

    IPage<NoteVO> selectNotePageWithUser(Page<NoteVO> page,
                                         @Param("userId") Long userId,
                                         @Param("currentUserId") Long currentUserId,
                                         @Param("keyword") String keyword,
                                         @Param("noteType") Integer noteType,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate);

    IPage<NoteVO> searchNotesWithUser(Page<NoteVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("currentUserId") Long currentUserId);

    IPage<NoteVO> selectNotePageAdmin(Page<NoteVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("noteType") Integer noteType,
                                      @Param("isPublic") Integer isPublic,
                                      @Param("isDeleted") Integer isDeleted);

    List<NoteVO> selectTopNotes(@Param("limit") Integer limit);

    @Select("SELECT IFNULL(SUM(view_count), 0) FROM t_note_item WHERE is_deleted = 0")
    Long sumViewCount();

    IPage<NoteVO> selectRecycleBinNotes(Page<NoteVO> page, @Param("userId") Long userId);

    @Update("UPDATE t_note_item SET is_deleted = 0, deleted_at = NULL, updated_at = NOW() WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 1")
    int restoreUserNote(@Param("id") Long id, @Param("userId") Long userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM t_note_item WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 1")
    int hardDeleteUserNote(@Param("id") Long id, @Param("userId") Long userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM t_note_item WHERE user_id = #{userId} AND is_deleted = 1")
    int clearUserRecycleBin(@Param("userId") Long userId);

    IPage<NoteVO> selectFollowingFeed(Page<NoteVO> page, @Param("currentUserId") Long currentUserId);

    List<NoteVO> selectTrendingNotes(@Param("limit") Integer limit);

    IPage<NoteVO> selectRecommendedNotes(Page<NoteVO> page,
                                         @Param("currentUserId") Long currentUserId,
                                         @Param("tag") String tag);
}
