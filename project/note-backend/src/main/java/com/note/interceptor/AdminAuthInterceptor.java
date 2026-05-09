package com.note.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.util.AdminJwtUtil;
import com.note.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminJwtUtil adminJwtUtil;
    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(AdminJwtUtil adminJwtUtil) {
        this.adminJwtUtil = adminJwtUtil;
        this.objectMapper = new ObjectMapper();
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
