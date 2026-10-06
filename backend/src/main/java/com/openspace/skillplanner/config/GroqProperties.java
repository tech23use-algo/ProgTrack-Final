package com.openspace.skillplanner.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq")
public record GroqProperties(
        String endpoint,
        String apiKey,
        String model,
        double temperature,
        int maxTokens,
        Duration timeout) {
}
