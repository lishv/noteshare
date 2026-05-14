package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.entity.NoteTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {

    @Delete("DELETE FROM t_note_tag WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") Long noteId);

    @Select("SELECT tag_id FROM t_note_tag WHERE note_id = #{noteId}")
    List<Long> selectTagIdsByNoteId(@Param("noteId") Long noteId);

    @Select("SELECT note_id FROM t_note_tag WHERE tag_id = #{tagId}")
    List<Long> selectNoteIdsByTagId(@Param("tagId") Long tagId);

    @Delete("DELETE FROM t_note_tag WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);
}
