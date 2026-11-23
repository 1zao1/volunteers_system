package com.volunteer.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.volunteer.common.JwtUtil;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;

        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);

        // 公开接口：活动列表与详情允许游客（未登录）访问
        String path = req.getRequestURI();
        boolean publicRoute = "GET".equalsIgnoreCase(req.getMethod())
                && (path.equals("/api/activities")
                    || path.matches("/api/activities/\\d+")
                    || path.equals("/api/activities/categories")
                    || path.matches("/api/organizers/\\d+")
                    || path.equals("/api/stats/overview"));
        if (publicRoute && (token == null || token.isBlank())) {
            return true;
        }

        try {
            Claims claims = jwtUtil.parse(token);
            req.setAttribute("userId", Long.valueOf(claims.getSubject()));
            Object role = claims.get("role");
            req.setAttribute("role", role);

            // 管理端接口只允许组织者访问（平台不再设管理员）
            String uri = path;
            if (uri.startsWith("/api/admin")) {
                String r = String.valueOf(role);
                if (!"organizer".equals(r)) {
                    resp.setStatus(403);
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().write("{\"code\":403,\"message\":\"没有权限访问管理端\"}");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }
    }
}
