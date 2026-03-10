package com.taxworkbench.api.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ServerInstanceHeaderFilter extends OncePerRequestFilter {
    public static final String SERVER_INSTANCE_HEADER = "X-Server-Instance-Key";
    private final String serverInstanceKey = "srv-" + UUID.randomUUID();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader(SERVER_INSTANCE_HEADER, serverInstanceKey);
        filterChain.doFilter(request, response);
    }
}
