package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.User;
import com.note.entity.UserFollow;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.UserFollowMapper;
import com.note.mapper.UserMapper;
import com.note.vo.ActiveUserVO;
import com.note.vo.FollowStatsVO;
import com.note.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public FollowService(UserFollowMapper userFollowMapper, UserMapper userMapper, NotificationService notificationService) {
        this.userFollowMapper = userFollowMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public void followUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能关注自己");
        }

        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null || targetUser.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "目标用户不存在或已被禁用");
        }

        if (userFollowMapper.checkIsFollowing(currentUserId, targetUserId) > 0) {
            return; // 幂等成功
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerId(currentUserId);
        follow.setFollowingId(targetUserId);
        follow.setCreatedAt(LocalDateTime.now());
        userFollowMapper.insert(follow);

        // 发送关注站内通知
        try {
            User follower = userMapper.selectById(currentUserId);
            String followerName = follower != null ? follower.getUsername() : "用户";
            notificationService.sendNotification(
                    targetUserId,
                    currentUserId,
                    "FOLLOW",
                    followerName + " 关注了你",
                    "关注了你的账号，去 TA 的主页看看吧~",
                    "USER",
                    currentUserId
            );
        } catch (Exception ignored) {}
    }

    @Transactional
    public void unfollowUser(Long currentUserId, Long targetUserId) {
        userFollowMapper.deleteByFollowerAndFollowing(currentUserId, targetUserId);
    }

    public boolean checkIsFollowing(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            return false;
        }
        return userFollowMapper.checkIsFollowing(currentUserId, targetUserId) > 0;
    }

    public FollowStatsVO getFollowStats(Long targetUserId, Long currentUserId) {
        long followerCount = userFollowMapper.countFollowers(targetUserId);
        long followingCount = userFollowMapper.countFollowing(targetUserId);
        boolean isFollowing = checkIsFollowing(currentUserId, targetUserId);

        return new FollowStatsVO(followerCount, followingCount, isFollowing);
    }

    public PageResult<ActiveUserVO> getFollowedUsers(Long followerId, Integer page, Integer size) {
        if (page == null) page = 1;
        if (size == null) size = 50;

        Page<ActiveUserVO> p = new Page<>(page, size);
        IPage<ActiveUserVO> res = userFollowMapper.selectFollowedUsers(p, followerId);
        return PageResult.of(res.getTotal(), page, size, res.getRecords());
    }

    public PageResult<ActiveUserVO> getFollowers(Long targetUserId, Long currentUserId, Integer page, Integer size) {
        if (page == null) page = 1;
        if (size == null) size = 50;

        Page<ActiveUserVO> p = new Page<>(page, size);
        IPage<ActiveUserVO> res = userFollowMapper.selectFollowers(p, targetUserId, currentUserId);
        return PageResult.of(res.getTotal(), page, size, res.getRecords());
    }
}
