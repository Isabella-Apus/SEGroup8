package com.segroup8.platform.interceptor;

import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OptionalJwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    public OptionalJwtInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return true;
        }
        try {
            String token = auth.substring(7);
            Claims claims = jwtUtils.parseToken(token);
            Object uidObj = claims.get("uid");
            Long uid = uidObj instanceof Number ? ((Number) uidObj).longValue() : Long.valueOf(uidObj.toString());
            UserContext.setUserId(uid);
        } catch (Exception ignored) {
            UserContext.clear();
            // Public endpoints should not fail when token is absent/invalid.
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
