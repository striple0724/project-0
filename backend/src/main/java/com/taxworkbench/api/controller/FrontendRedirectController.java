package com.taxworkbench.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class FrontendRedirectController {
    private final String frontendBaseUrl;

    public FrontendRedirectController(
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping({"/", "/login"})
    public ResponseEntity<Void> redirectToFrontendLogin() {
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(base + "/login"))
                .build();
    }
}
