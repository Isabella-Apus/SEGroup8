package com.segroup8.identity.config;

import com.segroup8.identity.security.AuthenticationInterceptor;
import com.segroup8.identity.security.InternalServiceInterceptor;
import com.segroup8.identity.security.RequestTraceInterceptor;
import com.segroup8.security.JwtTokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final RequestTraceInterceptor trace;
    private final AuthenticationInterceptor authentication;
    private final InternalServiceInterceptor internal;

    public WebConfig(RequestTraceInterceptor trace, AuthenticationInterceptor authentication,
            InternalServiceInterceptor internal) {
        this.trace = trace;
        this.authentication = authentication;
        this.internal = internal;
    }

    @Bean
    static JwtTokenVerifier jwtTokenVerifier(@Value("${app.jwt-secret}") String secret) {
        return new JwtTokenVerifier(secret);
    }

    @Bean
    static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(trace).addPathPatterns("/**");
        registry.addInterceptor(authentication)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login");
        registry.addInterceptor(internal).addPathPatterns("/internal/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*").allowCredentials(true);
    }
}
