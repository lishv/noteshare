package com.note.service;

import com.note.dto.LoginDTO;
import com.note.dto.RegisterDTO;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.UserMapper;
import com.note.util.JwtUtil;
import com.note.vo.LoginVO;
import com.note.vo.UserVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final VerificationCodeService verificationCodeService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${security.login.max-fail-count:5}")
    private Integer maxFailCount;

    @Value("${security.login.lock-duration:600}")
    private Integer lockDuration;

    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate, VerificationCodeService verificationCodeService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.verificationCodeService = verificationCodeService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public LoginVO register(RegisterDTO dto) {
        verificationCodeService.verifyCode(dto.getEmail(), "register", dto.getVerificationCode());

        User existingUser = userMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }

        User existingEmail = userMapper.selectByEmail(dto.getEmail());
        if (existingEmail != null) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATE);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar("");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(convertToVO(user));

        return loginVO;
    }

    public LoginVO login(LoginDTO dto, String ipAddress) {
        String failKey = LOGIN_FAIL_KEY_PREFIX + dto.getUsername() + ":" + ipAddress;
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        if (failCount != null && failCount >= maxFailCount) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            incrementFailCount(failKey);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        if (user.getStatus() == 2) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            incrementFailCount(failKey);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        redisTemplate.delete(failKey);

        userMapper.updateLastActiveAt(user.getId(), LocalDateTime.now());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        user.setLastActiveAt(LocalDateTime.now());
        loginVO.setUser(convertToVO(user));

        return loginVO;
    }

    private void incrementFailCount(String key) {
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, lockDuration, TimeUnit.SECONDS);
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
