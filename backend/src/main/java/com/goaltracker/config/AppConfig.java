package com.goaltracker.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.web.client.RestClient;

import com.goaltracker.ai.GroqProperties;
@Configuration @EnableConfigurationProperties(GroqProperties.class) public class AppConfig { @Bean RestClient.Builder restClientBuilder(){return RestClient.builder();} }
