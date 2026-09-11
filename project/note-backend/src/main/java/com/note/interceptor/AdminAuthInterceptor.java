package com.note.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.util.AdminJwtUtil;
import com.note.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.Date;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminJwtUtil adminJwtUtil;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public AdminAuthInterceptor(AdminJwtUtil adminJwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.adminJwtUtil = adminJwtUtil;
        this.objectMapper = new ObjectMapper();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            handleAuthError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        try {
            adminJwtUtil.validateToken(token);
        } catch (BusinessException e) {
            handleAuthError(response, e.getCode(), e.getMessage());
            return false;
        }

        Long adminId = adminJwtUtil.getAdminIdFromToken(token);
        String adminUsername = adminJwtUtil.getUsernameFromToken(token);

        // 检查密码是否被修改或重置（Token 签发时间早于密码修改时间）
        try {
            Object invalidBefore = redisTemplate.opsForValue().get("admin:token:invalid_before:" + adminId);
            if (invalidBefore != null) {
                long invalidTime = 0L;
                if (invalidBefore instanceof Number) {
                    invalidTime = ((Number) invalidBefore).longValue();
                } else if (invalidBefore instanceof String) {
                    invalidTime = Long.parseLong((String) invalidBefore);
                }
                Date iat = adminJwtUtil.getIssuedAtFromToken(token);
                if (iat != null && iat.getTime() <= invalidTime) {
                    handleAuthError(response, ErrorCode.TOKEN_EXPIRED.getCode(), "密码已修改，请重新登录");
                    return false;
                }
            }
        } catch (Exception ignored) {}

        request.setAttribute("adminId", adminId);
        request.setAttribute("adminUsername", adminUsername);

        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void handleAuthError(HttpServletResponse response, ErrorCode errorCode) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(errorCode);
        String json = objectMapper.writeValueAsString(result);
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }

    private void handleAuthError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(code, message);
        String json = objectMapper.writeValueAsString(result);
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
