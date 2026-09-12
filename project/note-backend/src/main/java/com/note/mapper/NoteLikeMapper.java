package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.entity.NoteLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLike> {

    @Select("SELECT COUNT(1) FROM t_note_like WHERE user_id = #{userId} AND note_id = #{noteId}")
    int existsByUserAndNote(@Param("userId") Long userId, @Param("noteId") Long noteId);

    @Delete("DELETE FROM t_note_like WHERE user_id = #{userId} AND note_id = #{noteId}")
    int deleteByUserAndNote(@Param("userId") Long userId, @Param("noteId") Long noteId);

    @Select("SELECT COUNT(1) FROM t_note_like WHERE note_id = #{noteId}")
    long countByNoteId(@Param("noteId") Long noteId);
}
