package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.entity.Tag;
import com.note.vo.PopularTagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT * FROM t_tag WHERE user_id = #{userId} ORDER BY name")
    List<Tag> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT t.name AS name, COUNT(DISTINCT nt.note_id) AS noteCount " +
            "FROM t_tag t " +
            "INNER JOIN t_note_tag nt ON t.id = nt.tag_id " +
            "INNER JOIN t_note_item n ON nt.note_id = n.id " +
            "WHERE n.is_deleted = 0 AND n.is_public = 1 " +
            "GROUP BY t.name " +
            "HAVING COUNT(DISTINCT nt.note_id) > 0 " +
            "ORDER BY noteCount DESC, t.name ASC " +
            "LIMIT #{limit}")
    List<PopularTagVO> selectPopularTags(@Param("limit") Integer limit);
}
