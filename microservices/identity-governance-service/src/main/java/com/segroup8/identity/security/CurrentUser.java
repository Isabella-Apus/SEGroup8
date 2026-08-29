package com.segroup8.identity.security;

import com.segroup8.identity.api.ApiException;
import com.segroup8.security.JwtPrincipal;

public final class CurrentUser {
    private static final ThreadLocal<JwtPrincipal> VALUE = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(JwtPrincipal principal) {
        VALUE.set(principal);
    }

    public static JwtPrincipal require() {
        JwtPrincipal principal = VALUE.get();
        if (principal == null) {
            throw new ApiException(401, "未登录或登录已过期");
        }
        return principal;
    }

    public static void clear() {
        VALUE.remove();
    }
}
