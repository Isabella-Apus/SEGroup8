package com.segroup8.order;

import com.segroup8.order.ApiModels.CreateOrderItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class HttpDownstreamGateway implements DownstreamGateway {
    private final RestClient catalog;
    private final RestClient finance;
    private final String internalToken;

    HttpDownstreamGateway(RestClient.Builder builder,
            @Value("${downstream.catalog-url}") String catalogUrl,
            @Value("${downstream.finance-url}") String financeUrl,
            @Value("${security.internal-service-token}") String internalToken) {
        this.catalog = builder.clone().baseUrl(catalogUrl).build();
        this.finance = builder.clone().baseUrl(financeUrl).build();
        this.internalToken = internalToken;
    }

    @Override
    public Reservation reserve(String reservationId, long buyerUserId, List<CreateOrderItem> items) {
        try {
            Reservation response = catalog.post().uri("/internal/inventory/reservations")
                    .header("X-Internal-Service-Token", internalToken)
                    .header("Idempotency-Key", reservationId)
                    .body(Map.of("reservationId", reservationId, "buyerUserId", buyerUserId, "items", items))
                    .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new OrderException("INVENTORY_RESERVATION_FAILED", "Inventory could not be reserved", 409);
                    }).body(Reservation.class);
            if (response == null || response.items() == null || response.items().isEmpty()) {
                throw new OrderException("INVALID_CATALOG_RESPONSE", "Catalog returned no item snapshots", 502);
            }
            return response;
        } catch (OrderException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new OrderException("CATALOG_TEMPORARILY_UNAVAILABLE", "Catalog is temporarily unavailable", 503);
        }
    }

    @Override public void confirmReservation(String key) { inventoryCommand(key, "confirm"); }
    @Override public void releaseReservation(String key) { inventoryCommand(key, "release"); }

    private void inventoryCommand(String key, String action) {
        catalog.post().uri("/internal/inventory/reservations/{id}/" + action, key)
                .header("X-Internal-Service-Token", internalToken).header("Idempotency-Key", key + ":" + action)
                .retrieve().toBodilessEntity();
    }

    @Override
    public Quote quote(String key, long buyerUserId, BigDecimal totalAmount, Long voucherId) {
        try {
            Quote result = finance.post().uri("/internal/finance/quotes")
                    .header("X-Internal-Service-Token", internalToken).header("Idempotency-Key", key)
                    .body(Map.of("quoteRequestId", key, "buyerUserId", buyerUserId,
                            "totalAmount", totalAmount, "voucherId", voucherId == null ? "" : voucherId))
                    .retrieve().body(Quote.class);
            return result == null ? new Quote(totalAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO) : result;
        } catch (RestClientException ex) {
            throw new OrderException("FINANCE_QUOTE_UNAVAILABLE", "Finance quote is temporarily unavailable", 503);
        }
    }

    @Override public RemoteResult debit(String key, long user, BigDecimal amount, String mode, String channel) {
        return financeWrite("/internal/finance/debits", key,
                Map.of("paymentRequestId", key, "userId", user, "amount", amount,
                        "payMode", mode == null ? "COIN" : mode, "payChannel", channel == null ? "" : channel));
    }
    @Override public RemoteResult paymentResult(String key) { return financeQuery("/internal/finance/debits/{id}", key); }
    @Override public RemoteResult refund(String key, long orderId, long user, BigDecimal amount) {
        return financeWrite("/internal/finance/refunds", key,
                Map.of("refundRequestId", key, "orderId", orderId, "userId", user, "amount", amount));
    }
    @Override public RemoteResult refundResult(String key) { return financeQuery("/internal/finance/refunds/{id}", key); }
    @Override public RemoteResult settle(String key, long orderId, long sellerUserId, BigDecimal amount) {
        return financeWrite("/internal/finance/settlements", key,
                Map.of("settlementRequestId", key, "orderId", orderId, "sellerUserId", sellerUserId, "amount", amount));
    }
    @Override public RemoteResult settlementResult(String key) {
        return financeQuery("/internal/finance/settlements/{id}", key);
    }
    @Override public void releaseVoucher(String key, Long voucherId, long user) {
        if (voucherId == null) return;
        financeWrite("/internal/finance/vouchers/releases", key,
                Map.of("releaseRequestId", key, "voucherId", voucherId, "userId", user));
    }

    private RemoteResult financeWrite(String uri, String key, Object body) {
        try {
            Map<?, ?> response = finance.post().uri(uri).header("X-Internal-Service-Token", internalToken)
                    .header("Idempotency-Key", key).body(body).retrieve().body(Map.class);
            return parse(response);
        } catch (RestClientException ex) {
            return RemoteResult.UNKNOWN;
        }
    }

    private RemoteResult financeQuery(String uri, String key) {
        try {
            return parse(finance.get().uri(uri, key).header("X-Internal-Service-Token", internalToken)
                    .retrieve().body(Map.class));
        } catch (RestClientException ex) {
            return RemoteResult.UNKNOWN;
        }
    }

    private RemoteResult parse(Map<?, ?> body) {
        if (body == null) return RemoteResult.UNKNOWN;
        String status = String.valueOf(body.get("status"));
        if ("SUCCEEDED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) return RemoteResult.SUCCEEDED;
        if ("FAILED".equalsIgnoreCase(status)) return RemoteResult.FAILED;
        return RemoteResult.UNKNOWN;
    }
}
