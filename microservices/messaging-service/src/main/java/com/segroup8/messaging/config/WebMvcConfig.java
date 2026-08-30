package com.segroup8.messaging.config;

import com.segroup8.messaging.security.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtAuthenticationInterceptor jwt;
    public WebMvcConfig(JwtAuthenticationInterceptor jwt) { this.jwt = jwt; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwt).addPathPatterns("/api/chat/**", "/api/notifications/**");
    }
}
