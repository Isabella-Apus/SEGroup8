package com.segroup8.secondhand.client;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpIdentityGateway implements IdentityGateway {
    private final RestClient client;
    private final String internalToken;

    public HttpIdentityGateway(@Qualifier("identityRestClient") RestClient client,
            @Value("${security.internal-token}") String internalToken) {
        this.client = client;
        this.internalToken = internalToken;
    }

    @Override
    public AddressSnapshot resolveAddress(long userId, Long addressId) {
        try {
            AddressEnvelope envelope = client.get()
                    .uri(builder -> builder.path("/internal/users/{userId}/address-snapshot")
                            .queryParamIfPresent("addressId", Optional.ofNullable(addressId))
                            .build(userId))
                    .header("X-Internal-Service-Token", internalToken)
                    .header("X-Request-Id", "secondhand-address-" + UUID.randomUUID())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IdentityServiceUnavailableException(
                                "identity-governance-service returned HTTP " + response.getStatusCode());
                    })
                    .body(AddressEnvelope.class);
            if (envelope == null) {
                throw new IdentityServiceUnavailableException("identity-governance-service returned no response");
            }
            if (envelope.code() != 0) {
                if (envelope.code() == 404) {
                    throw new IdentityAddressNotFoundException(envelope.message());
                }
                throw new IdentityServiceUnavailableException(
                        "identity-governance-service rejected address lookup: " + envelope.message());
            }
            if (envelope.data() == null || envelope.data().id() == null
                    || blank(envelope.data().receiverName()) || blank(envelope.data().receiverPhone())
                    || blank(envelope.data().province()) || blank(envelope.data().city())
                    || blank(envelope.data().detailAddress())) {
                throw new IdentityServiceUnavailableException(
                        "identity-governance-service returned an incomplete address snapshot");
            }
            return envelope.data().toSnapshot();
        } catch (IdentityAddressNotFoundException | IdentityServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new IdentityServiceUnavailableException("identity-governance-service address lookup failed", exception);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    record AddressEnvelope(int code, String message, AddressData data) {
    }

    record AddressData(Long id, Long userId, String receiverName, String receiverPhone,
            String province, String city, String detailAddress) {
        AddressSnapshot toSnapshot() {
            return new AddressSnapshot(id, userId == null ? 0 : userId, receiverName, receiverPhone,
                    province, city, detailAddress);
        }
    }
}
