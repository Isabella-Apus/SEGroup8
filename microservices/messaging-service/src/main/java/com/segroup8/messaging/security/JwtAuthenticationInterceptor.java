package com.segroup8.messaging.security;

import com.segroup8.messaging.access.AccessPolicy;
import com.segroup8.messaging.common.ApiException;
import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import com.segroup8.security.JwtVerificationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {
    public static final String PRINCIPAL_ATTRIBUTE = MessagingPrincipal.class.getName();
    private final JwtTokenVerifier verifier;
    private final AccessPolicy accessPolicy;
    public JwtAuthenticationInterceptor(JwtTokenVerifier verifier, AccessPolicy accessPolicy) {
        this.verifier = verifier; this.accessPolicy = accessPolicy;
    }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String header = request.getHeader("Authorization");
            JwtPrincipal jwt = verifier.verifyAuthorizationHeader(header);
            accessPolicy.requireActive(jwt.userId());
            request.setAttribute(PRINCIPAL_ATTRIBUTE,
                    new MessagingPrincipal(jwt));
            return true;
        } catch (JwtVerificationException ex) {
            throw new ApiException(401, "Invalid or expired JWT");
        }
    }
}
