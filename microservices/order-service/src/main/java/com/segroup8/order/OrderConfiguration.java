package com.segroup8.order;

import com.segroup8.security.JwtTokenVerifier;
import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
class OrderConfiguration {
    @Bean
    JwtTokenVerifier jwtTokenVerifier(@Value("${security.jwt.secret}") String secret) {
        return new JwtTokenVerifier(secret);
    }

    @Bean
    RestClientCustomizer orderRestClientTimeouts(
            @Value("${downstream.connect-timeout:1s}") Duration connectTimeout,
            @Value("${downstream.read-timeout:2s}") Duration readTimeout) {
        return builder -> {
            var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connectTimeout).build());
            factory.setReadTimeout(readTimeout);
            builder.requestFactory(factory);
        };
    }
}
