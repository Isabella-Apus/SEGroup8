package com.segroup8.security;

/**
 * 微服务 JWT 校验失败时使用的统一异常类型。
 */
public class JwtVerificationException extends RuntimeException {

    public JwtVerificationException(String message) {
        super(message);
    }

    public JwtVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
