package com.segroup8.secondhand.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.secondhand.common.ApiResponse;
import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import com.segroup8.security.JwtVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenVerifier verifier;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String secret, ObjectMapper objectMapper) {
        this.verifier = new JwtTokenVerifier(secret);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        boolean publicRequest = isPublicRequest(request);
        if ((authorization == null || authorization.isBlank()) && publicRequest) {
            filterChain.doFilter(request, response);
            return;
        }
        if (skipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            JwtPrincipal principal = verifier.verifyAuthorizationHeader(authorization);
            request.setAttribute(AuthenticationSupport.USER_ATTRIBUTE,
                    new AuthenticatedUser(principal.userId(), principal.username(), principal.role()));
            filterChain.doFilter(request, response);
        } catch (JwtVerificationException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.failure(401, "登录状态无效或已过期"));
        }
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return path.equals("/api/secondhand/list")
                || path.matches("/api/secondhand/detail/\\d+")
                || path.matches("/api/secondhand/seller-public/\\d+(/products)?")
                || path.matches("/api/secondhand/trade/auction/product/\\d+");
    }

    private boolean skipAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/internal/");
    }
}
