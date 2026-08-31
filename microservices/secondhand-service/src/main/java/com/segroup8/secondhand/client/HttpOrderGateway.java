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
    private final AddressGateway addresses;

    public HttpOrderGateway(@Qualifier("orderRestClient") RestClient client,
            @Value("${security.internal-token}") String internalToken, AddressGateway addresses) {
        this.client = client;
        this.internalToken = internalToken;
        this.addresses = addresses;
    }

    @Override
    public OrderReceipt createSecondhandOrder(TradeOrderRequest request) {
        try {
            if (request.addressId() == null) {
                throw new OrderServiceUnavailableException("addressId is required for secondhand order");
            }
            AddressGateway.AddressSnapshot address = addresses.requireOwnedAddress(
                    request.buyerUserId(), request.addressId(), request.orderBusinessKey());
            OrderData order = client.post().uri("/internal/orders/secondhand")
                    .header("X-Internal-Service-Token", internalToken)
                    .header("Idempotency-Key", request.orderBusinessKey())
                    .body(CreateOrderCommand.from(request, address))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                        throw new OrderServiceUnavailableException("order-service returned HTTP " + response.getStatusCode());
                    })
                    .body(OrderData.class);
            if (order == null || order.id() == null) {
                throw new OrderServiceUnavailableException("order-service returned an incomplete response");
            }
            return order.toReceipt();
        } catch (OrderServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new OrderServiceUnavailableException("order-service create request failed", exception);
        }
    }

    @Override
    public Optional<OrderReceipt> findByBusinessKey(String businessKey) {
        try {
            OrderData order = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/orders/by-business-key/{key}").build(businessKey))
                    .header("X-Internal-Service-Token", internalToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new OrderNotFoundException();
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new OrderServiceUnavailableException("order-service lookup returned HTTP " + response.getStatusCode());
                    })
                    .body(OrderData.class);
            return order == null || order.id() == null ? Optional.empty() : Optional.of(order.toReceipt());
        } catch (OrderNotFoundException notFound) {
            return Optional.empty();
        } catch (OrderServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new OrderServiceUnavailableException("order-service lookup failed", exception);
        }
    }

    record CreateOrderCommand(String tradeType, String tradeId, long buyerUserId, long sellerUserId,
            long productId, String productName, java.math.BigDecimal price, String receiverName,
            String receiverPhone, String receiverProvince, String receiverCity,
            String receiverDetailAddress, String remark) {
        static CreateOrderCommand from(TradeOrderRequest request, AddressGateway.AddressSnapshot address) {
            return new CreateOrderCommand(request.tradeType(), request.tradeId(), request.buyerUserId(),
                    request.sellerUserId(), request.productId(), "Secondhand product #" + request.productId(),
                    request.price(), address.receiverName(), address.receiverPhone(), address.province(),
                    address.city(), address.detailAddress(),
                    request.remark());
        }
    }

    record OrderData(Long id, String orderNo, String orderStatus) {
        OrderReceipt toReceipt() {
            return new OrderReceipt(id, orderNo, orderStatus == null ? "PENDING_PAY" : orderStatus);
        }
    }

    private static final class OrderNotFoundException extends RuntimeException {
    }
}
