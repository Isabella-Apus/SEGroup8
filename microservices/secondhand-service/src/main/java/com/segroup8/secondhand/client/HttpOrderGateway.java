package com.segroup8.secondhand.client;

import com.segroup8.secondhand.domain.TradeOrderRequest;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpOrderGateway implements OrderGateway {
    private final RestClient client;
    private final String internalToken;

    public HttpOrderGateway(@Qualifier("orderRestClient") RestClient client,
            @Value("${security.internal-token}") String internalToken) {
        this.client = client;
        this.internalToken = internalToken;
    }

    @Override
    public OrderReceipt createSecondhandOrder(TradeOrderRequest request) {
        try {
            OrderEnvelope envelope = client.post().uri("/internal/orders/secondhand")
                    .header("X-Internal-Service-Token", internalToken)
                    .header("X-Idempotency-Key", request.orderBusinessKey())
                    .body(new CreateOrderCommand(request.tradeType(), request.tradeId(), request.orderBusinessKey(),
                            request.productId(), request.buyerUserId(), request.sellerUserId(), request.price(),
                            request.addressId(), request.remark()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                        throw new OrderServiceUnavailableException("order-service returned HTTP " + response.getStatusCode());
                    })
                    .body(OrderEnvelope.class);
            if (envelope == null || envelope.data() == null || envelope.data().orderId() == null) {
                throw new OrderServiceUnavailableException("order-service returned an incomplete response");
            }
            return envelope.data().toReceipt();
        } catch (OrderServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new OrderServiceUnavailableException("order-service create request failed", exception);
        }
    }

    @Override
    public Optional<OrderReceipt> findByBusinessKey(String businessKey) {
        try {
            OrderEnvelope envelope = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/orders/by-business-key/{key}").build(businessKey))
                    .header("X-Internal-Service-Token", internalToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new OrderNotFoundException();
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new OrderServiceUnavailableException("order-service lookup returned HTTP " + response.getStatusCode());
                    })
                    .body(OrderEnvelope.class);
            return envelope == null || envelope.data() == null || envelope.data().orderId() == null
                    ? Optional.empty() : Optional.of(envelope.data().toReceipt());
        } catch (OrderNotFoundException notFound) {
            return Optional.empty();
        } catch (OrderServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new OrderServiceUnavailableException("order-service lookup failed", exception);
        }
    }

    record CreateOrderCommand(String tradeType, String tradeId, String orderBusinessKey, long productId,
            long buyerId, long sellerId, java.math.BigDecimal price, Long addressId, String remark) {
    }

    record OrderEnvelope(Integer code, String message, OrderData data) {
    }

    record OrderData(Long orderId, String orderNo, String status) {
        OrderReceipt toReceipt() {
            return new OrderReceipt(orderId, orderNo, status == null ? "PENDING_PAY" : status);
        }
    }

    private static final class OrderNotFoundException extends RuntimeException {
    }
}
