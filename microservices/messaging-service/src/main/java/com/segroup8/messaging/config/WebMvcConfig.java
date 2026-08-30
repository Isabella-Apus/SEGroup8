package com.segroup8.messaging.config;

import com.segroup8.messaging.security.JwtAuthenticationInterceptor;
import com.segroup8.messaging.security.InternalServiceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtAuthenticationInterceptor jwt;
    private final InternalServiceInterceptor internal;
    public WebMvcConfig(JwtAuthenticationInterceptor jwt, InternalServiceInterceptor internal) {
        this.jwt = jwt; this.internal = internal;
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwt).addPathPatterns("/api/chat/**", "/api/notifications/**");
        registry.addInterceptor(internal).addPathPatterns("/internal/**");
    }
}
