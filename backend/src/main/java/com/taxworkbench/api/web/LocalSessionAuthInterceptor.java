package com.taxworkbench.api.web;

import com.taxworkbench.api.config.AppSettings;
import com.taxworkbench.api.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LocalSessionAuthInterceptor implements HandlerInterceptor {
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
        if (session == null || session.getAttribute(UserAccountService.SESSION_USER_KEY) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Login required\"}");
            return false;
        }
        return true;
    }
}
