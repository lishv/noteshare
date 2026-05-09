package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.entity.AdminLoginFailRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminLoginFailRecordMapper extends BaseMapper<AdminLoginFailRecord> {

    @Select("SELECT * FROM t_admin_login_fail_record WHERE username = #{username} AND ip_address = #{ipAddress} LIMIT 1")
    AdminLoginFailRecord selectByUsernameAndIp(@Param("username") String username, @Param("ipAddress") String ipAddress);
}
