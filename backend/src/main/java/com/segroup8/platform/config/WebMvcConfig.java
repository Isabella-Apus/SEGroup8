package com.segroup8.platform.config;

import com.segroup8.platform.interceptor.JwtAuthInterceptor;
import com.segroup8.platform.interceptor.IdempotencyInterceptor;
import com.segroup8.platform.interceptor.InternalServiceInterceptor;
import com.segroup8.platform.interceptor.OptionalJwtInterceptor;
import com.segroup8.platform.interceptor.TraceIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final @NonNull JwtAuthInterceptor jwtAuthInterceptor;
    private final @NonNull OptionalJwtInterceptor optionalJwtInterceptor;
    private final @NonNull TraceIdInterceptor traceIdInterceptor;
    private final @NonNull IdempotencyInterceptor idempotencyInterceptor;
    private final @NonNull InternalServiceInterceptor internalServiceInterceptor;

    public WebMvcConfig(@NonNull JwtAuthInterceptor jwtAuthInterceptor,
            @NonNull OptionalJwtInterceptor optionalJwtInterceptor,
            @NonNull TraceIdInterceptor traceIdInterceptor,
            @NonNull IdempotencyInterceptor idempotencyInterceptor,
            @NonNull InternalServiceInterceptor internalServiceInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.optionalJwtInterceptor = optionalJwtInterceptor;
        this.traceIdInterceptor = traceIdInterceptor;
        this.idempotencyInterceptor = idempotencyInterceptor;
        this.internalServiceInterceptor = internalServiceInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(internalServiceInterceptor)
                .addPathPatterns("/internal/blocks/**");
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(optionalJwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html");

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/product/list",
                        "/api/product/detail/*",
                        "/api/shop/public/*",
                        "/api/shop/public/*/products",
                        "/api/secondhand/list",
                        "/api/secondhand/detail/*",
                        "/api/secondhand/seller-public/*",
                        "/api/secondhand/seller-public/*/products",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html");

        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/api/order/**", "/api/admin/orders/**");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
