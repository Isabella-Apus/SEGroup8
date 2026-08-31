package com.segroup8.secondhand.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpAddressGateway implements AddressGateway {
    private final RestClient client;
    private final String internalToken;

    public HttpAddressGateway(@Qualifier("identityRestClient") RestClient client,
            @Value("${security.internal-token}") String internalToken) {
        this.client = client;
        this.internalToken = internalToken;
    }

    @Override
    public AddressSnapshot requireOwnedAddress(long userId, long addressId, String requestId) {
        try {
            AddressEnvelope envelope = client.get()
                    .uri("/internal/users/{userId}/addresses/{addressId}", userId, addressId)
                    .header("X-Internal-Service-Token", internalToken)
                    .header("X-Request-Id", requestId + ":address")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new AddressServiceUnavailableException(
                                "identity-governance-service returned HTTP " + response.getStatusCode());
                    })
                    .body(AddressEnvelope.class);
            if (envelope == null || envelope.code() != 0 || envelope.data() == null) {
                throw new AddressServiceUnavailableException("identity address response is incomplete");
            }
            Map<String, Object> data = envelope.data();
            return new AddressSnapshot(required(data, "receiverName"), required(data, "receiverPhone"),
                    required(data, "province"), required(data, "city"), required(data, "detailAddress"));
        } catch (AddressServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AddressServiceUnavailableException("identity address lookup failed", exception);
        }
    }

    private String required(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new AddressServiceUnavailableException("identity address response misses " + key);
        }
        return String.valueOf(value);
    }

    record AddressEnvelope(int code, Map<String, Object> data) {
    }

    static final class AddressServiceUnavailableException extends RuntimeException {
        AddressServiceUnavailableException(String message) { super(message); }
        AddressServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
