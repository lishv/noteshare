package com.note.service;

import com.note.dto.ChangePasswordDTO;
import com.note.service.VerificationCodeService;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.UserFollowMapper;
import com.note.mapper.UserMapper;
import com.note.vo.ActiveUserVO;
import com.note.vo.PageResult;
import com.note.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;
    private final SensitiveWordService sensitiveWordService;
    private final RedisTemplate<String, Object> redisTemplate;

    public UserService(UserMapper userMapper, UserFollowMapper userFollowMapper, VerificationCodeService verificationCodeService, SensitiveWordService sensitiveWordService, RedisTemplate<String, Object> redisTemplate) {
        this.userMapper = userMapper;
        this.userFollowMapper = userFollowMapper;
        this.verificationCodeService = verificationCodeService;
        this.sensitiveWordService = sensitiveWordService;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (dto.getVerificationCode() != null && !dto.getVerificationCode().isEmpty()
                && dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            verificationCodeService.verifyCode(dto.getEmail(), "changePassword", dto.getVerificationCode());
        } else if (dto.getOldPassword() != null && !dto.getOldPassword().isEmpty()) {
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.PASSWORD_ERROR);
            }
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供旧密码或邮箱验证码");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        if (user.getStatus() == 2) {
            user.setStatus(1);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        try {
            redisTemplate.opsForValue().set("user:token:invalid_before:" + user.getId(), System.currentTimeMillis(), 7, java.util.concurrent.TimeUnit.DAYS);
            Set<String> keys = redisTemplate.keys("login:fail:" + user.getUsername() + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {}
    }

    @Transactional
    public UserVO updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setAvatar(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return convertToVO(user);
    }

    @Transactional
    public UserVO updateBio(Long userId, String bio) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String trimmedBio = bio != null ? bio.trim() : "";
        if (trimmedBio.length() > 30) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "个性签名不能超过30个字符");
        }
        if (!trimmedBio.isEmpty() && sensitiveWordService.containsSensitiveWord(trimmedBio)) {
            throw new BusinessException(ErrorCode.BIO_CONTAINS_SENSITIVE_WORD);
        }
        user.setBio(trimmedBio);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return convertToVO(user);
    }

    public PageResult<ActiveUserVO> getActiveUsers(Long currentUserId, Integer page, Integer size, String keyword) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        // 如果提供了搜索关键词：全局检索所有作者/用户
        if (keyword != null && !keyword.trim().isEmpty()) {
            Page<ActiveUserVO> userPage = new Page<>(page, size);
            IPage<ActiveUserVO> result = userMapper.searchUsers(userPage, keyword.trim(), null);
            return PageResult.of(result.getTotal(), page, size, result.getRecords());
        }

        // 如果用户已登录：侧边栏仅展示该用户已关注的作者，并在第一页首位保留作者本人便于切换管理
        if (currentUserId != null) {
            Page<ActiveUserVO> pageParam = new Page<>(page, size);
            IPage<ActiveUserVO> followedResult = userFollowMapper.selectFollowedUsers(pageParam, currentUserId);
            List<ActiveUserVO> records = new ArrayList<>(followedResult.getRecords());

            if (page == 1) {
                boolean containsSelf = records.stream().anyMatch(u -> u.getId().equals(currentUserId));
                if (!containsSelf) {
                    ActiveUserVO selfVO = userMapper.selectActiveUserById(currentUserId);
                    if (selfVO != null) {
                        records.add(0, selfVO);
                    }
                }
            }

            // 返回该用户真实的关注作者总数（不把作者本人计入关注数）
            long total = followedResult.getTotal();
            return PageResult.of(total, page, size, records);
        }

        // 未登录访客：展示活跃用户推荐
        Page<ActiveUserVO> userPage = new Page<>(page, size);
        IPage<ActiveUserVO> result = userMapper.searchUsers(userPage, null, null);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setLastActiveAt(user.getLastActiveAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
