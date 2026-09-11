package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.User;
import com.note.vo.ActiveUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE username = #{username} LIMIT 1")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_user WHERE email = #{email} LIMIT 1")
    User selectByEmail(@Param("email") String email);

    @Update("UPDATE t_user SET last_active_at = #{lastActiveAt} WHERE id = #{id}")
    int updateLastActiveAt(@Param("id") Long id, @Param("lastActiveAt") LocalDateTime lastActiveAt);

    IPage<ActiveUserVO> searchUsers(Page<ActiveUserVO> page, @Param("keyword") String keyword, @Param("status") Integer status);

    ActiveUserVO selectActiveUserById(@Param("userId") Long userId);
}
