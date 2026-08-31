package com.segroup8.secondhand.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class IdentityClientConfiguration {
    @Bean("identityRestClient")
    RestClient identityRestClient(RestClient.Builder builder,
            @Value("${clients.identity.base-url}") String baseUrl,
            @Value("${clients.identity.connect-timeout}") Duration connectTimeout,
            @Value("${clients.identity.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
