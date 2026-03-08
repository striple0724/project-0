package com.taxworkbench.api.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final LocalSessionAuthInterceptor localSessionAuthInterceptor;

    public WebMvcConfig(LocalSessionAuthInterceptor localSessionAuthInterceptor) {
        this.localSessionAuthInterceptor = localSessionAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localSessionAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/h2-console/**"
                );
    }
}
