package com.segroup8.messaging.security;

import com.segroup8.security.JwtTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {
    @Bean JwtTokenVerifier jwtTokenVerifier(@Value("${app.jwt.secret}") String secret) {
        return new JwtTokenVerifier(secret);
    }
}
