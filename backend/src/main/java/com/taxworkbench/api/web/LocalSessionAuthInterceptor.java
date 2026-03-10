package com.taxworkbench.api.web;

import com.taxworkbench.api.config.AppSettings;
import com.taxworkbench.api.dto.auth.LoginResponse;
import com.taxworkbench.api.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LocalSessionAuthInterceptor implements HandlerInterceptor {
    private static final String ADMIN_USER_ID = "admin";
    private final AppSettings appSettings;

    public LocalSessionAuthInterceptor(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!appSettings.getSecurity().isRequireAuth()) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute(UserAccountService.SESSION_USER_KEY);
        if (!(sessionUser instanceof LoginResponse loginUser)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Login required\"}");
            return false;
        }

        String path = request.getRequestURI();
        if (isAdminOnlyPath(path) && !ADMIN_USER_ID.equalsIgnoreCase(loginUser.userId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Admin only\"}");
            return false;
        }
        return true;
    }

    private boolean isAdminOnlyPath(String path) {
        return path.startsWith("/api/v1/admin/")
                || path.startsWith("/api/v1/seed-admin/")
                || (path.startsWith("/api/v1/work-items/bulk-jobs/") && path.endsWith("/failures"));
    }
}
