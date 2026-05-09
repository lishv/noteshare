package com.note.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.entity.OperationLog;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.OperationLogMapper;
import com.note.mapper.UserMapper;
import com.note.vo.ActiveUserVO;
import com.note.vo.PageResult;
import com.note.vo.UserVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AdminUserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OperationLogMapper operationLogMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String BLACKLIST_PREFIX = "user:blacklist:";

    public AdminUserService(UserMapper userMapper, RedisTemplate<String, Object> redisTemplate,
                            OperationLogMapper operationLogMapper) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.operationLogMapper = operationLogMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public PageResult<ActiveUserVO> listUsers(Integer page, Integer size, String keyword) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        Page<ActiveUserVO> userPage = new Page<>(page, size);
        IPage<ActiveUserVO> result = userMapper.searchUsers(userPage, keyword);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public UserVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Transactional
    public void toggleUserStatus(Long userId, Integer status, Long adminId, String adminUsername, String ipAddress) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        if (status == 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + user.getUsername(), "1", 7, TimeUnit.DAYS);
        } else {
            redisTemplate.delete(BLACKLIST_PREFIX + user.getUsername());
        }

        logOperation(adminId, adminUsername, "USER_MANAGE",
                (status == 0 ? "禁用" : "启用") + "用户: " + user.getUsername(),
                "USER", userId, ipAddress);
    }

    @Transactional
    public void unlockUser(Long userId, Long adminId, String adminUsername, String ipAddress) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        String failKey = "login:fail:" + user.getUsername() + ":*";
        redisTemplate.delete(failKey);

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "解锁用户: " + user.getUsername(), "USER", userId, ipAddress);
    }

    @Transactional
    public void resetUserPassword(Long userId, Long adminId, String adminUsername, String ipAddress) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode("123456"));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "重置用户密码: " + user.getUsername(), "USER", userId, ipAddress);
    }

    @Transactional
    public void batchToggleStatus(List<Long> userIds, Integer status, Long adminId, String adminUsername, String ipAddress) {
        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setStatus(status);
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);

                if (status == 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + user.getUsername(), "1", 7, TimeUnit.DAYS);
                } else {
                    redisTemplate.delete(BLACKLIST_PREFIX + user.getUsername());
                }
            }
        }

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "批量" + (status == 0 ? "禁用" : "启用") + "用户，数量: " + userIds.size(),
                "USER", null, ipAddress);
    }

    private void logOperation(Long adminId, String adminUsername, String operationType,
                              String description, String targetType, Long targetId, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setOperationType(operationType);
        log.setDescription(description);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setLastActiveAt(user.getLastActiveAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
