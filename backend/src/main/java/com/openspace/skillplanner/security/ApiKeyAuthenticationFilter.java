package com.openspace.skillplanner.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspace.skillplanner.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final AuthProperties properties;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(AuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/")
                || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String configuredKey = properties.apiKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String suppliedKey = request.getHeader("X-API-Key");
        if (suppliedKey != null && MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(),
                Map.of("code", "UNAUTHORIZED", "message", "A valid API key is required."));
    }
}
