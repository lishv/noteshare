package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.MainLink;
import com.note.vo.MainLinkVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MainLinkMapper extends BaseMapper<MainLink> {

    @Select("SELECT * FROM t_main_link WHERE user_id = #{userId} LIMIT 1")
    MainLink selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE t_main_link SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM t_main_link WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    IPage<MainLinkVO> selectLinkPageAdmin(Page<MainLinkVO> page, @Param("keyword") String keyword);
}
