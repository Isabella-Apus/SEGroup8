package com.segroup8.identity.security;

import com.segroup8.identity.api.ApiException;
import com.segroup8.identity.service.IdentityService;
import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import com.segroup8.security.JwtVerificationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    private final JwtTokenVerifier verifier;
    private final IdentityService service;

    public AuthenticationInterceptor(JwtTokenVerifier verifier, IdentityService service) {
        this.verifier = verifier;
        this.service = service;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            JwtPrincipal principal = verifier.verifyAuthorizationHeader(request.getHeader("Authorization"));
            service.assertActive(principal.userId());
            CurrentUser.set(principal);
            return true;
        } catch (JwtVerificationException ex) {
            throw new ApiException(401, "无效或过期的登录令牌");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
