package com.segroup8.catalogshop;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class RequestLoggingFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String requestId=clean(request.getHeader("X-Request-Id"));String traceId=clean(request.getHeader("X-Trace-Id"));
        MDC.put("requestId",requestId);MDC.put("traceId",traceId);response.setHeader("X-Request-Id",requestId);
        try{chain.doFilter(request,response);}finally{MDC.clear();}
    }
    private String clean(String value){return value==null||value.isBlank()?UUID.randomUUID().toString():value.substring(0,Math.min(128,value.length()));}
}
