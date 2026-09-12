package com.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.UserFollow;
import com.note.vo.ActiveUserVO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Delete("DELETE FROM t_user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int deleteByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Select("SELECT COUNT(1) FROM t_user_follow WHERE following_id = #{userId}")
    long countFollowers(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM t_user_follow WHERE follower_id = #{userId}")
    long countFollowing(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM t_user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int checkIsFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    IPage<ActiveUserVO> selectFollowedUsers(Page<ActiveUserVO> page, @Param("followerId") Long followerId);

    IPage<ActiveUserVO> selectFollowers(Page<ActiveUserVO> page, @Param("targetUserId") Long targetUserId, @Param("currentUserId") Long currentUserId);
}
