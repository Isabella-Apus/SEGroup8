package com.segroup8.secondhand.security;

import com.segroup8.secondhand.common.DomainException;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthenticationSupport {
    public static final String USER_ATTRIBUTE = AuthenticatedUser.class.getName();

    private AuthenticationSupport() {
    }

    public static AuthenticatedUser requireUser(HttpServletRequest request) {
        Object value = request.getAttribute(USER_ATTRIBUTE);
        if (value instanceof AuthenticatedUser user) {
            return user;
        }
        throw new DomainException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "AUTH_REQUIRED", "请先登录");
    }
}
