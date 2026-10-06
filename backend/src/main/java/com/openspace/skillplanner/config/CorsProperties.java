package com.openspace.skillplanner.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openspace.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
