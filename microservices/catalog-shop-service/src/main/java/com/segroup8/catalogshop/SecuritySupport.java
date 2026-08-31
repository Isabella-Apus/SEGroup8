package com.segroup8.catalogshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.security.JwtPrincipal;
import com.segroup8.security.JwtTokenVerifier;
import com.segroup8.security.JwtVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

record AuthenticatedUser(long userId, String username, String role) {
    AuthenticatedUser requireRole(String... allowed) {
        if (!Set.of(allowed).contains(role)) throw new ApiException("ROLE_FORBIDDEN", "当前角色无权执行此操作", org.springframework.http.HttpStatus.FORBIDDEN);
        return this;
    }
}

final class AuthenticationSupport {
    static final String ATTRIBUTE = AuthenticatedUser.class.getName();
    private AuthenticationSupport() {}
    static AuthenticatedUser require(HttpServletRequest request) {
        Object user=request.getAttribute(ATTRIBUTE);
        if(user instanceof AuthenticatedUser authenticated)return authenticated;
        throw new ApiException("AUTH_REQUIRED","请先登录",org.springframework.http.HttpStatus.UNAUTHORIZED);
    }
}

@Component
class IdentityAccessVerifier {
    private final RestClient client;
    private final ObjectMapper json;
    private final String url;
    private final String mode;
    private final String internalToken;

    IdentityAccessVerifier(ObjectMapper json,
            @Value("${catalog-shop.identity.status-check-url}") String url,
            @Value("${catalog-shop.identity.status-check-mode:introspect}") String mode,
            @Value("${catalog-shop.internal-token}") String internalToken,
            @Value("${catalog-shop.http.connect-timeout-ms:1500}") int connectTimeout,
            @Value("${catalog-shop.http.read-timeout-ms:2500}") int readTimeout) {
        this.json=json;this.url=url;this.mode=mode;this.internalToken=internalToken;
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        this.client=RestClient.builder().requestFactory(factory).build();
    }

    void requireActive(String authorization, JwtPrincipal principal) {
        try {
            JsonNode root;
            if("profile".equalsIgnoreCase(mode)) {
                root=client.get().uri(url).header(HttpHeaders.AUTHORIZATION,authorization).retrieve().body(JsonNode.class);
            } else {
                String token=authorization.substring("Bearer ".length()).trim();
                root=client.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Service-Token",internalToken)
                    .body(json.createObjectNode().put("token",token)).retrieve().body(JsonNode.class);
            }
            JsonNode data=root==null?null:root.path("data");
            boolean active="profile".equalsIgnoreCase(mode)
                ? root.path("code").asInt(-1)==0 && !"BANNED".equalsIgnoreCase(data.path("status").asText())
                : root.path("code").asInt(-1)==0 && data.path("active").asBoolean(false);
            long id=data==null?-1:data.path("userId").asLong(data.path("id").asLong(-1));
            String role=data==null?"":data.path("role").asText();
            if(!active||id!=principal.userId()||!principal.role().equals(role))
                throw new ApiException("ACCOUNT_INACTIVE","用户已被禁用或身份状态已变化",org.springframework.http.HttpStatus.UNAUTHORIZED);
        } catch(ApiException e){throw e;}
        catch(RestClientException e){throw new ApiException("IDENTITY_UNAVAILABLE","身份状态服务暂时不可用",org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);}
    }
}

@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenVerifier tokens;private final IdentityAccessVerifier access;private final ObjectMapper json;
    JwtAuthenticationFilter(@Value("${security.jwt.secret}") String secret,IdentityAccessVerifier access,ObjectMapper json){this.tokens=new JwtTokenVerifier(secret);this.access=access;this.json=json;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String authorization=request.getHeader(HttpHeaders.AUTHORIZATION);
        if(skip(request)||(publicRequest(request)&&(authorization==null||authorization.isBlank()))){chain.doFilter(request,response);return;}
        try{
            JwtPrincipal principal=tokens.verifyAuthorizationHeader(authorization);
            access.requireActive(authorization,principal);
            request.setAttribute(AuthenticationSupport.ATTRIBUTE,new AuthenticatedUser(principal.userId(),principal.username(),principal.role()));
            chain.doFilter(request,response);
        }catch(JwtVerificationException e){write(response,org.springframework.http.HttpStatus.UNAUTHORIZED,"INVALID_TOKEN","登录状态无效或已过期");}
        catch(ApiException e){write(response,e.status,e.code,e.getMessage());}
    }
    private boolean publicRequest(HttpServletRequest r){if(!"GET".equalsIgnoreCase(r.getMethod()))return false;String p=r.getRequestURI();return p.equals("/api/category/tree")||p.equals("/api/product/list")||p.equals("/api/product/search")||p.matches("/api/product/detail/\\d+")||p.startsWith("/api/shop/public/")||p.equals("/api/search/hot");}
    private boolean skip(HttpServletRequest r){String p=r.getRequestURI();return p.startsWith("/internal/")||p.startsWith("/actuator/")||p.startsWith("/v3/api-docs")||p.startsWith("/swagger-ui");}
    private void write(HttpServletResponse response,org.springframework.http.HttpStatus status,String error,String message)throws IOException{response.setStatus(status.value());response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.setCharacterEncoding("UTF-8");json.writeValue(response.getWriter(),new ApiResult<Void>(status.value(),message,null,error));}
}
