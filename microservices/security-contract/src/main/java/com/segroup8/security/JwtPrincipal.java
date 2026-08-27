package com.segroup8.security;

/**
 * 身份治理服务签发、其他微服务消费的最小 JWT 身份载荷。
 */
public record JwtPrincipal(long userId, String username, String role) {
}
