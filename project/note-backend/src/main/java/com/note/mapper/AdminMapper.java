package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.Admin;
import com.note.vo.AdminManageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

    @Select("SELECT * FROM t_admin WHERE username = #{username} LIMIT 1")
    Admin selectByUsername(@Param("username") String username);

    @Update("UPDATE t_admin SET last_login_at = #{lastLoginAt} WHERE id = #{id}")
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    IPage<AdminManageVO> selectAdminPage(Page<AdminManageVO> page, @Param("keyword") String keyword);
}
