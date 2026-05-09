package com.note.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.util.JwtUtil;
import com.note.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());

        // 公开接口白名单（无需token）
        if (path.startsWith("/user/auth/") ||
            path.equals("/user/users/active") ||
            path.startsWith("/user/main-link/") ||
            path.startsWith("/user/notes/user/") ||
            path.equals("/user/search") ||
            path.startsWith("/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/webjars") ||
            path.equals("/openapi.json")) {
            return true;
        }

        // GET /user/notes/{id} 和 POST /user/notes/{id}/view 也公开（浏览笔记、增加浏览量）
        // 注意：PUT /user/notes/{id} 和 DELETE /user/notes/{id} 需要认证，不能放行
        String method = request.getMethod();
        if (path.startsWith("/user/notes/") && !path.startsWith("/user/notes/user/")) {
            String sub = path.substring("/user/notes/".length());
            if ("GET".equals(method) && sub.matches("\\d+$")) return true;
            if ("POST".equals(method) && sub.matches("\\d+/view$")) return true;
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

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}