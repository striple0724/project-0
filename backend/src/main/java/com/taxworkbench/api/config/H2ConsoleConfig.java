package com.taxworkbench.api.config;

import jakarta.servlet.Servlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.h2.server.web.JakartaWebServlet")
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
public class H2ConsoleConfig {

    @Bean
    ServletRegistrationBean<Servlet> h2ConsoleServletRegistration(
            @Value("${spring.h2.console.path:/h2-console}") String consolePath
    ) {
        String normalizedPath = normalizePath(consolePath);
        Servlet h2Servlet = createH2Servlet();

        ServletRegistrationBean<Servlet> registrationBean =
                new ServletRegistrationBean<>(h2Servlet, normalizedPath + "/*");
        registrationBean.setName("H2Console");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }

    private Servlet createH2Servlet() {
        try {
            Class<?> servletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            return (Servlet) servletClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize H2 console servlet", e);
        }
    }

    private String normalizePath(String rawPath) {
        String path = (rawPath == null || rawPath.isBlank()) ? "/h2-console" : rawPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
