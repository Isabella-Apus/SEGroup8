package com.segroup8.messaging.security;

import com.segroup8.security.JwtPrincipal;

public record MessagingPrincipal(JwtPrincipal jwt) {
    public long userId() { return jwt.userId(); }
}
