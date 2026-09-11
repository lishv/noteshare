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

    @Select("SELECT n.id, n.user_id, u.username, u.avatar, n.title, n.note_type, n.location_or_url, n.content, n.summary, n.cover_image, n.view_count, n.is_pinned as isPinned, n.is_public as isPublic, n.created_at, n.updated_at " +
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
}
