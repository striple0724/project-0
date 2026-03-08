package com.taxworkbench.api;

import com.taxworkbench.api.config.AppSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    private final AppSettings appSettings;

    public SecurityConfig(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/actuator/health",
                            "/api/v1/auth/**",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/h2-console/**"
                    ).permitAll();
                    auth.anyRequest().permitAll();
                });

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> originPatterns = appSettings.getCors().getAllowedOriginPatterns();
        if (originPatterns != null && !originPatterns.isEmpty()) {
            config.setAllowedOriginPatterns(originPatterns);
        } else {
            List<String> origins = appSettings.getCors().getAllowedOrigins().isEmpty()
                    ? List.of("http://localhost:5173")
                    : appSettings.getCors().getAllowedOrigins();
            config.setAllowedOrigins(origins);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Request-Id", "ETag", "Location"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
