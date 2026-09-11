package com.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.dto.AdminCreateDTO;
import com.note.dto.AdminEditDTO;
import com.note.entity.Admin;
import com.note.entity.OperationLog;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.AdminMapper;
import com.note.mapper.OperationLogMapper;
import com.note.vo.AdminManageVO;
import com.note.vo.PageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AdminManageService {

    private final AdminMapper adminMapper;
    private final OperationLogMapper operationLogMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${default.adminPassword:admin123}")
    private String defaultAdminPassword;

    private static final String ADMIN_LOGIN_FAIL_KEY = "admin:login:fail:";

    public AdminManageService(AdminMapper adminMapper, OperationLogMapper operationLogMapper, RedisTemplate<String, Object> redisTemplate) {
        this.adminMapper = adminMapper;
        this.operationLogMapper = operationLogMapper;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public PageResult<AdminManageVO> listAdmins(Integer page, Integer size, String keyword) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        Page<AdminManageVO> adminPage = new Page<>(page, size);
        IPage<AdminManageVO> result = adminMapper.selectAdminPage(adminPage, keyword);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Transactional
    public void createAdmin(AdminCreateDTO dto, Long operatorId, String operatorUsername, String ipAddress) {
        Admin existing = adminMapper.selectByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException(ErrorCode.ADMIN_USERNAME_DUPLICATE);
        }

        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setEmail(dto.getEmail());
        admin.setPhone(dto.getPhone());
        admin.setRole(dto.getRole() != null ? dto.getRole() : 2);
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.insert(admin);

        logOperation(operatorId, operatorUsername, "ADMIN_MANAGE",
                "创建管理员: " + dto.getUsername(), "ADMIN", admin.getId(), ipAddress);
    }

    @Transactional
    public void editAdmin(Long adminId, AdminEditDTO dto, Long operatorId, String operatorUsername, String ipAddress) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if (dto.getEmail() != null) admin.setEmail(dto.getEmail());
        if (dto.getPhone() != null) admin.setPhone(dto.getPhone());
        if (dto.getRole() != null) admin.setRole(dto.getRole());
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.updateById(admin);

        logOperation(operatorId, operatorUsername, "ADMIN_MANAGE",
                "编辑管理员: " + admin.getUsername(), "ADMIN", adminId, ipAddress);
    }

    @Transactional
    public void toggleAdminStatus(Long adminId, Integer status, Long operatorId, String operatorUsername, String ipAddress) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if (adminId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.CANNOT_OPERATE_SELF);
        }

        if ("admin".equals(admin.getUsername())) {
            throw new BusinessException(ErrorCode.ADMIN_ACCESS_DENIED, "默认超级管理员不可被禁用");
        }

        if (admin.getRole() == 1 && status == 0) {
            Long superCount = adminMapper.selectCount(new LambdaQueryWrapper<Admin>()
                    .eq(Admin::getRole, 1)
                    .eq(Admin::getStatus, 1));
            if (superCount <= 1) {
                throw new BusinessException(ErrorCode.ADMIN_LAST_SUPER);
            }
        }

        admin.setStatus(status);
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.updateById(admin);

        if (status == 1) {
            try {
                Set<String> keys = redisTemplate.keys(ADMIN_LOGIN_FAIL_KEY + admin.getUsername() + ":*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception ignored) {}
        } else {
            try {
                redisTemplate.opsForValue().set("admin:token:invalid_before:" + admin.getId(), System.currentTimeMillis(), 7, java.util.concurrent.TimeUnit.DAYS);
            } catch (Exception ignored) {}
        }

        logOperation(operatorId, operatorUsername, "ADMIN_MANAGE",
                (status == 0 ? "禁用" : "启用") + "管理员: " + admin.getUsername(),
                "ADMIN", adminId, ipAddress);
    }

    @Transactional
    public void resetAdminPassword(Long adminId, Long operatorId, String operatorUsername, String ipAddress) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if ("admin".equals(admin.getUsername())) {
            throw new BusinessException(ErrorCode.ADMIN_ACCESS_DENIED, "默认超级管理员密码不可被重置");
        }

        admin.setPassword(passwordEncoder.encode(defaultAdminPassword != null ? defaultAdminPassword : "admin123"));
        admin.setStatus(1);
        admin.setUpdatedAt(LocalDateTime.now());
        adminMapper.updateById(admin);

        try {
            redisTemplate.opsForValue().set("admin:token:invalid_before:" + admin.getId(), System.currentTimeMillis(), 7, java.util.concurrent.TimeUnit.DAYS);
            Set<String> keys = redisTemplate.keys(ADMIN_LOGIN_FAIL_KEY + admin.getUsername() + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {}

        logOperation(operatorId, operatorUsername, "ADMIN_MANAGE",
                "重置管理员密码: " + admin.getUsername(), "ADMIN", adminId, ipAddress);
    }

    @Transactional
    public void deleteAdmin(Long adminId, Long operatorId, String operatorUsername, String ipAddress) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }

        if ("admin".equals(admin.getUsername())) {
            throw new BusinessException(ErrorCode.ADMIN_ACCESS_DENIED, "超级管理员不可被删除");
        }

        if (adminId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.CANNOT_OPERATE_SELF);
        }

        adminMapper.deleteById(adminId);

        logOperation(operatorId, operatorUsername, "ADMIN_MANAGE",
                "删除管理员: " + admin.getUsername(), "ADMIN", adminId, ipAddress);
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
}
