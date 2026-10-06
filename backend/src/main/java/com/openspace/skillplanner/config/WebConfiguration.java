package com.openspace.skillplanner.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final CorsProperties corsProperties;

    public WebConfiguration(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = corsProperties.allowedOrigins();
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-API-Key", "Authorization")
                .allowCredentials(false)
                .maxAge(3600);
    }

}
