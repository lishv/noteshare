package com.note.service;

import com.note.dto.ResetPasswordDTO;
import com.note.dto.SendCodeDTO;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.UserMapper;
import com.note.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    void resetPasswordUpdatesEncodedPasswordAndInvalidatesAccess() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("$2a$10$old");
        user.setStatus(2);

        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setEmail(" alice@example.com ");
        dto.setVerificationCode(" 123456 ");
        dto.setNewPassword("newpass123");

        Set<String> failKeys = Set.of("login:fail:alice:127.0.0.1");
        when(userMapper.selectByEmail("alice@example.com")).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys("login:fail:alice:*")).thenReturn(failKeys);

        authService.resetPassword(dto);

        verify(verificationCodeService).verifyCode("alice@example.com", "resetPassword", "123456");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(1, saved.getStatus());
        assertEquals("alice@example.com", saved.getEmail());
        assertTrue(new BCryptPasswordEncoder().matches("newpass123", saved.getPassword()));
        verify(valueOperations).set(
                eq("user:token:invalid_before:7"),
                any(),
                eq(7L),
                eq(TimeUnit.DAYS)
        );
        verify(redisTemplate).delete(failKeys);
    }

    @Test
    void resetPasswordFailsBeforeUpdatingUserWhenVerificationCodeIsInvalid() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setEmail("alice@example.com");
        dto.setVerificationCode("123456");
        dto.setNewPassword("newpass123");
        BusinessException failure = new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        doThrow(failure).when(verificationCodeService)
                .verifyCode("alice@example.com", "resetPassword", "123456");

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> authService.resetPassword(dto)
        );

        assertEquals(ErrorCode.VERIFICATION_CODE_ERROR.getCode(), thrown.getCode());
        verify(userMapper, never()).selectByEmail(any());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void resetCodeIsNotSentForUnknownEmail() {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setEmail("unknown@example.com");
        dto.setScene("resetPassword");
        when(userMapper.selectByEmail("unknown@example.com")).thenReturn(null);

        authService.sendVerificationCode(dto);

        verify(verificationCodeService, never()).sendCode(any(), any());
    }

    @Test
    void verificationCodeRejectsUnknownScene() {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setEmail("alice@example.com");
        dto.setScene("customScene");

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> authService.sendVerificationCode(dto)
        );

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), thrown.getCode());
        assertEquals("验证码场景无效", thrown.getMessage());
        verify(verificationCodeService, never()).sendCode(any(), any());
    }
}
