package com.segroup8.platform.interceptor;

import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    public JwtAuthInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }

        String token = auth.substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Object uidObj = claims.get("uid");
        Long uid = uidObj instanceof Number ? ((Number) uidObj).longValue() : Long.valueOf(uidObj.toString());
        UserContext.setUserId(uid);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
