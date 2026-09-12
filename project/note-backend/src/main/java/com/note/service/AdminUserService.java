package com.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.dto.UserCreateDTO;
import com.note.dto.UserUpdateDTO;
import com.note.entity.OperationLog;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.NoteFavoriteMapper;
import com.note.mapper.NoteItemMapper;
import com.note.mapper.OperationLogMapper;
import com.note.mapper.UserMapper;
import com.note.mapper.MainLinkMapper;
import com.note.vo.ActiveUserVO;
import com.note.vo.PageResult;
import com.note.vo.UserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final NoteItemMapper noteItemMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final MainLinkMapper mainLinkMapper;
    private final NotificationService notificationService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${default.userPassword:123456}")
    private String defaultUserPassword;

    private static final String BLACKLIST_PREFIX = "user:blacklist:";

    public AdminUserService(UserMapper userMapper, RedisTemplate<String, Object> redisTemplate,
                            OperationLogMapper operationLogMapper, NoteItemMapper noteItemMapper,
                            NoteFavoriteMapper noteFavoriteMapper,
                            MainLinkMapper mainLinkMapper,
                            NotificationService notificationService) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.operationLogMapper = operationLogMapper;
        this.noteItemMapper = noteItemMapper;
        this.noteFavoriteMapper = noteFavoriteMapper;
        this.mainLinkMapper = mainLinkMapper;
        this.notificationService = notificationService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Cacheable(value = "activeUsers", key = "'list:' + #page + ':' + #size + ':' + (#keyword != null ? #keyword : '') + ':' + (#status != null ? #status : '')")
    public PageResult<ActiveUserVO> listUsers(Integer page, Integer size, String keyword, Integer status) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        Page<ActiveUserVO> userPage = new Page<>(page, size);
        IPage<ActiveUserVO> result = userMapper.searchUsers(userPage, keyword, status);
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
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
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
            redisTemplate.opsForValue().set("user:token:invalid_before:" + user.getId(), System.currentTimeMillis(), 7, TimeUnit.DAYS);
        } else {
            redisTemplate.delete(BLACKLIST_PREFIX + user.getUsername());
            try {
                java.util.Set<String> keys = redisTemplate.keys("login:fail:" + user.getUsername() + ":*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception ignored) {}
        }

        logOperation(adminId, adminUsername, "USER_MANAGE",
                (status == 0 ? "禁用" : "启用") + "用户: " + user.getUsername(),
                "USER", userId, ipAddress);

        if (status == 0) {
            try {
                notificationService.sendNotification(
                        userId,
                        null,
                        "SYSTEM",
                        "系统通知：您的账号已被管理员禁用",
                        "您的账号状态已变更为禁用状态，如有疑问请联系管理员。",
                        "USER",
                        userId
                );
            } catch (Exception ignored) {}
        } else {
            try {
                notificationService.sendNotification(
                        userId,
                        null,
                        "SYSTEM",
                        "系统通知：您的账号已被管理员恢复正常",
                        "您的账号状态已恢复正常，欢迎继续使用笔记分享平台。",
                        "USER",
                        userId
                );
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
    public void unlockUser(Long userId, Long adminId, String adminUsername, String ipAddress) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        try {
            java.util.Set<String> keys = redisTemplate.keys("login:fail:" + user.getUsername() + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {}

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "解锁用户: " + user.getUsername(), "USER", userId, ipAddress);

        try {
            notificationService.sendNotification(
                    userId,
                    null,
                    "SYSTEM",
                    "系统通知：您的账号已被管理员解除锁定",
                    "您的账号已被管理员成功解锁，您可以正常登录与发表笔记。",
                    "USER",
                    userId
            );
        } catch (Exception ignored) {}
    }

    @Transactional
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
    public void resetUserPassword(Long userId, Long adminId, String adminUsername, String ipAddress) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(defaultUserPassword != null ? defaultUserPassword : "123456"));
        user.setStatus(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        try {
            redisTemplate.opsForValue().set("user:token:invalid_before:" + user.getId(), System.currentTimeMillis(), 7, TimeUnit.DAYS);
            java.util.Set<String> keys = redisTemplate.keys("login:fail:" + user.getUsername() + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {}

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "重置用户密码: " + user.getUsername(), "USER", userId, ipAddress);

        try {
            notificationService.sendNotification(
                    userId,
                    null,
                    "SYSTEM",
                    "系统通知：您的密码已被管理员重置",
                    "管理员已将您的密码重置为初始密码 (123456)，请尽快登录个人设置中心修改密码以保证安全。",
                    "USER",
                    userId
            );
        } catch (Exception ignored) {}
    }

    @Transactional
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
    public void batchToggleStatus(List<Long> userIds, Integer status, Long adminId, String adminUsername, String ipAddress) {
        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setStatus(status);
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);

                if (status == 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + user.getUsername(), "1", 7, TimeUnit.DAYS);
                    redisTemplate.opsForValue().set("user:token:invalid_before:" + user.getId(), System.currentTimeMillis(), 7, TimeUnit.DAYS);
                    try {
                        notificationService.sendNotification(
                                userId,
                                null,
                                "SYSTEM",
                                "系统通知：您的账号已被管理员禁用",
                                "您的账号状态已变更为禁用状态，如有疑问请联系管理员。",
                                "USER",
                                userId
                        );
                    } catch (Exception ignored) {}
                } else {
                    redisTemplate.delete(BLACKLIST_PREFIX + user.getUsername());
                    try {
                        notificationService.sendNotification(
                                userId,
                                null,
                                "SYSTEM",
                                "系统通知：您的账号已被管理员恢复正常",
                                "您的账号状态已恢复正常，欢迎继续使用笔记分享平台。",
                                "USER",
                                userId
                        );
                    } catch (Exception ignored) {}
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

    @Transactional
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
    public void createUser(UserCreateDTO dto, Long adminId, String adminUsername, String ipAddress) {
        User existing = userMapper.selectByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }
        existing = userMapper.selectByEmail(dto.getEmail());
        if (existing != null) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setBio(dto.getBio());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "创建用户: " + dto.getUsername(), "USER", user.getId(), ipAddress);
    }

    @Transactional
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
    public UserVO updateUser(Long userId, UserUpdateDTO dto, Long adminId, String adminUsername, String ipAddress) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查用户名唯一性（如果修改了用户名）
        if (!user.getUsername().equals(dto.getUsername())) {
            User existing = userMapper.selectByUsername(dto.getUsername());
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
            }
            redisTemplate.delete(BLACKLIST_PREFIX + user.getUsername());
            redisTemplate.opsForValue().set("user:token:invalid_before:" + user.getId(), System.currentTimeMillis(), 7, TimeUnit.DAYS);
            user.setUsername(dto.getUsername());
        }

        // 检查邮箱唯一性（如果修改了邮箱）
        if (!user.getEmail().equals(dto.getEmail())) {
            User existing = userMapper.selectByEmail(dto.getEmail());
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
            }
            user.setEmail(dto.getEmail());
        }

        user.setPhone(dto.getPhone());
        user.setBio(dto.getBio() != null ? dto.getBio().trim() : null);

        // 如果修改了密码
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
            redisTemplate.opsForValue().set("user:token:invalid_before:" + user.getId(), System.currentTimeMillis(), 7, TimeUnit.DAYS);
        }

        // 如果修改了状态
        if (dto.getStatus() != null && !dto.getStatus().equals(user.getStatus())) {
            Integer oldStatus = user.getStatus();
            user.setStatus(dto.getStatus());
            if (dto.getStatus() == 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + user.getUsername(), "1", 7, TimeUnit.DAYS);
                redisTemplate.opsForValue().set("user:token:invalid_before:" + user.getId(), System.currentTimeMillis(), 7, TimeUnit.DAYS);
            } else if (oldStatus == 0) {
                redisTemplate.delete(BLACKLIST_PREFIX + user.getUsername());
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "编辑修改用户: " + user.getUsername(), "USER", user.getId(), ipAddress);

        return convertToVO(user);
    }

    @Transactional
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
    public void deleteUser(Long userId, Long adminId, String adminUsername, String ipAddress) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // Delete user's notes (physical delete)
        noteItemMapper.delete(new LambdaQueryWrapper<com.note.entity.NoteItem>()
                .eq(com.note.entity.NoteItem::getUserId, userId));
        // Delete user's favorites
        noteFavoriteMapper.delete(new LambdaQueryWrapper<com.note.entity.NoteFavorite>()
                .eq(com.note.entity.NoteFavorite::getUserId, userId));
        // Delete user's main link
        mainLinkMapper.delete(new LambdaQueryWrapper<com.note.entity.MainLink>()
                .eq(com.note.entity.MainLink::getUserId, userId));
        // Delete user
        userMapper.deleteById(userId);

        redisTemplate.delete(BLACKLIST_PREFIX + user.getUsername());

        logOperation(adminId, adminUsername, "USER_MANAGE",
                "删除用户: " + user.getUsername(), "USER", userId, ipAddress);
    }

    @Transactional
    @CacheEvict(value = {"activeUsers", "dashboard"}, allEntries = true)
    public void batchDeleteUsers(List<Long> userIds, Long adminId, String adminUsername, String ipAddress) {
        for (Long userId : userIds) {
            deleteUser(userId, adminId, adminUsername, ipAddress);
        }
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
