package com.segroup8.secondhand.security;

import com.segroup8.secondhand.common.DomainException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceAuthenticator {
    private final byte[] expectedToken;

    public InternalServiceAuthenticator(@Value("${security.internal-token}") String token) {
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String providedToken) {
        byte[] provided = providedToken == null ? new byte[0] : providedToken.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedToken, provided)) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "AUTH_INTERNAL", "内部服务凭证无效");
        }
    }
}
