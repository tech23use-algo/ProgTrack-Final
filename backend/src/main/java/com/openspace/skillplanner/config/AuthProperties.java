package com.openspace.skillplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openspace.auth")
public record AuthProperties(String apiKey) {
}
