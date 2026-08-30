package com.segroup8.messaging.security;

import com.segroup8.security.JwtPrincipal;

public record MessagingPrincipal(JwtPrincipal jwt, String token) {
    public long userId() { return jwt.userId(); }
}
