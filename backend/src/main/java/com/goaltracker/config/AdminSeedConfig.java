package com.goaltracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.goaltracker.model.Role;
import com.goaltracker.model.User;
import com.goaltracker.repository.UserRepository;

@Configuration
public class AdminSeedConfig {
    @Bean CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
        @Value("${ADMIN_EMAIL:}") String email, @Value("${ADMIN_PASSWORD:}") String password) {
        return args -> { if (!email.isBlank() && !password.isBlank() && !users.existsByEmail(email.toLowerCase())) users.save(new User(email.toLowerCase(), encoder.encode(password), "System Admin", Role.ADMIN)); };
    }
}
