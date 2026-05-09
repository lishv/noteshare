package com.note.service;

import com.note.dto.AdminChangePasswordDTO;
import com.note.dto.AdminLoginDTO;
import com.note.entity.Admin;
import com.note.entity.OperationLog;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.AdminMapper;
import com.note.mapper.OperationLogMapper;
import com.note.util.AdminJwtUtil;
import com.note.vo.AdminLoginVO;
import com.note.vo.AdminProfileVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class AdminAuthService {

    private final AdminMapper adminMapper;
    private final AdminJwtUtil adminJwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OperationLogMapper operationLogMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${security.login.max-fail-count:5}")
    private Integer maxFailCount;

    @Value("${security.login.lock-duration:600}")
    private Integer lockDuration;

    private static final String ADMIN_LOGIN_FAIL_KEY = "admin:login:fail:";

    public AdminAuthService(AdminMapper adminMapper, AdminJwtUtil adminJwtUtil,
                            RedisTemplate<String, Object> redisTemplate,
                            OperationLogMapper operationLogMapper) {
        this.adminMapper = adminMapper;
        this.adminJwtUtil = adminJwtUtil;
        this.redisTemplate = redisTemplate;
        this.operationLogMapper = operationLogMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public AdminLoginVO login(AdminLoginDTO dto, String ipAddress) {
        String failKey = ADMIN_LOGIN_FAIL_KEY + dto.getUsername() + ":" + ipAddress;
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        if (failCount != null && failCount >= maxFailCount) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_LOCKED);
        }

        Admin admin = adminMapper.selectByUsername(dto.getUsername());
        if (admin == null) {
            incrementFailCount(failKey);
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if (admin.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            incrementFailCount(failKey);
            throw new BusinessException(ErrorCode.ADMIN_PASSWORD_ERROR);
        }

        redisTemplate.delete(failKey);

        adminMapper.updateLastLoginAt(admin.getId(), LocalDateTime.now());

        boolean mustChangePassword = passwordEncoder.matches("admin123", admin.getPassword());

        String token = adminJwtUtil.generateToken(admin.getId(), admin.getUsername());

        logOperation(admin.getId(), admin.getUsername(), "LOGIN", "管理员登录: " + admin.getUsername(), null, null, ipAddress);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setMustChangePassword(mustChangePassword);
        vo.setAdmin(convertToProfileVO(admin));
        return vo;
    }

    public AdminProfileVO getProfile(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        return convertToProfileVO(admin);
    }

    @Transactional
    public void changePassword(Long adminId, AdminChangePasswordDTO dto, String ipAddress) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.ADMIN_PASSWORD_ERROR);
        }

        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.updateById(admin);

        logOperation(adminId, admin.getUsername(), "LOGIN", "管理员修改密码", null, null, ipAddress);
    }

    private void incrementFailCount(String key) {
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, lockDuration, TimeUnit.SECONDS);
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

    private AdminProfileVO convertToProfileVO(Admin admin) {
        AdminProfileVO vo = new AdminProfileVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setEmail(admin.getEmail());
        vo.setPhone(admin.getPhone());
        vo.setAvatar(admin.getAvatar());
        vo.setRole(admin.getRole());
        vo.setLastLoginAt(admin.getLastLoginAt());
        vo.setCreatedAt(admin.getCreatedAt());
        return vo;
    }
}
