package com.note.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.util.JwtUtil;
import com.note.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.Date;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthInterceptor(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = new ObjectMapper();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());

        // 公开接口白名单（无需token，但若携带则尝试解析用户身份）
        if (path.startsWith("/user/auth/") ||
            path.equals("/user/users/active") ||
            path.equals("/user/users/recommended") ||
            path.matches("^/user/users/\\d+$") ||
            path.equals("/user/tags/popular") ||
            path.equals("/user/share/qr-code") ||
            path.startsWith("/user/main-link/") ||
            path.startsWith("/user/notes/user/") ||
            path.equals("/user/search") ||
            path.startsWith("/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/webjars") ||
            path.equals("/openapi.json")) {
            tryExtractOptionalUser(request);
            return true;
        }

        // GET /user/notes/{id}、GET /user/notes/{id}/comments、GET /user/notes/trending、GET /user/notes/recommend 和 POST /user/notes/{id}/view 也公开
        String method = request.getMethod();
        if (path.startsWith("/user/notes/") && !path.startsWith("/user/notes/user/")) {
            String sub = path.substring("/user/notes/".length());
            if ("GET".equals(method) && (sub.matches("\\d+$") || sub.matches("\\d+/comments$") || "trending".equals(sub) || "recommend".equals(sub))) {
                tryExtractOptionalUser(request);
                return true;
            }
            if ("POST".equals(method) && sub.matches("\\d+/view$")) return true;
        }

        // GET /user/follow/{id}/status 查询关注状态 与 GET /user/follow/followers 允许可选 Token
        if (path.startsWith("/user/follow/") && "GET".equals(method)) {
            String sub = path.substring("/user/follow/".length());
            if (sub.matches("\\d+/status$") || "followers".equals(sub)) {
                tryExtractOptionalUser(request);
                return true;
            }
        }

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            handleAuthError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }
        try {
            jwtUtil.validateToken(token);
        } catch (BusinessException e) {
            handleAuthError(response, e.getCode(), e.getMessage());
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);

        // 1. 检查账号是否被管理员禁用（黑名单）
        try {
            Object blacklisted = redisTemplate.opsForValue().get("user:blacklist:" + username);
            if (blacklisted != null) {
                handleAuthError(response, ErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
                return false;
            }
        } catch (Exception ignored) {}

        // 2. 检查密码是否被修改或重置（Token 签发时间早于密码修改时间）
        try {
            Object invalidBefore = redisTemplate.opsForValue().get("user:token:invalid_before:" + userId);
            if (invalidBefore != null) {
                long invalidTime = 0L;
                if (invalidBefore instanceof Number) {
                    invalidTime = ((Number) invalidBefore).longValue();
                } else if (invalidBefore instanceof String) {
                    invalidTime = Long.parseLong((String) invalidBefore);
                }
                Date iat = jwtUtil.getIssuedAtFromToken(token);
                if (iat != null && iat.getTime() <= invalidTime) {
                    handleAuthError(response, ErrorCode.TOKEN_EXPIRED.getCode(), "密码已修改，请重新登录");
                    return false;
                }
            }
        } catch (Exception ignored) {}

        request.setAttribute("userId", userId);
        return true;
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

    private void tryExtractOptionalUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && !token.isEmpty()) {
            try {
                jwtUtil.validateToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                request.setAttribute("userId", userId);
            } catch (Exception ignored) {
            }
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}