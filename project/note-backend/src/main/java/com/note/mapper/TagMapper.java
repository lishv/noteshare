package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT * FROM t_tag WHERE user_id = #{userId} ORDER BY name")
    List<Tag> selectByUserId(@Param("userId") Long userId);
}
