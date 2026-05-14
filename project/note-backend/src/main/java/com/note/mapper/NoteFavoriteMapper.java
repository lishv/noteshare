package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.entity.NoteFavorite;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoteFavoriteMapper extends BaseMapper<NoteFavorite> {

    @Select("SELECT COUNT(*) FROM t_note_favorite WHERE user_id = #{userId} AND note_id = #{noteId}")
    int existsByUserAndNote(@Param("userId") Long userId, @Param("noteId") Long noteId);

    @Delete("DELETE FROM t_note_favorite WHERE user_id = #{userId} AND note_id = #{noteId}")
    int deleteByUserAndNote(@Param("userId") Long userId, @Param("noteId") Long noteId);

    @Select("SELECT note_id FROM t_note_favorite WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Long> selectFavoriteNoteIds(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_note_favorite WHERE note_id = #{noteId}")
    int countByNoteId(@Param("noteId") Long noteId);
}
