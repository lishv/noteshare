package com.note.service;

import com.note.dto.ChangePasswordDTO;
import com.note.service.VerificationCodeService;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.UserMapper;
import com.note.vo.ActiveUserVO;
import com.note.vo.PageResult;
import com.note.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;

    public UserService(UserMapper userMapper, VerificationCodeService verificationCodeService) {
        this.userMapper = userMapper;
        this.verificationCodeService = verificationCodeService;
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
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
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

    public PageResult<ActiveUserVO> getActiveUsers(Integer page, Integer size, String keyword) {
        if (page == null) page = 1;
        if (size == null) size = 20;

        Page<ActiveUserVO> userPage = new Page<>(page, size);
        IPage<ActiveUserVO> result = userMapper.searchUsers(userPage, keyword);

        return PageResult.of(result.getTotal(), page, size, result.getRecords());
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
