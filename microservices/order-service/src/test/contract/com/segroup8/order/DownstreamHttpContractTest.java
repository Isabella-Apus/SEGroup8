package com.segroup8.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.segroup8.order.ApiModels.CreateOrderItem;
import com.segroup8.order.DownstreamGateway.RemoteResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@Tag("CONTRACT")
class DownstreamHttpContractTest {
    @Test
    void catalogAndFinanceContractsUseStablePathsTokensAndIdempotencyKeys() throws Exception {
        List<String> calls = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/inventory/reservations", exchange -> respond(exchange, calls,
                "{\"reservationId\":\"reservation:create-1\",\"items\":[{\"productId\":10,\"productName\":\"Phone\",\"price\":100.00,\"quantity\":1,\"sellerUserId\":2,\"shopId\":20}]}"));
        server.createContext("/internal/checkout/quote", exchange -> respond(exchange, calls,
                "{\"payableAmount\":90.00,\"discountAmount\":10.00}"));
        server.createContext("/internal/payments", exchange -> respond(exchange, calls, "{\"status\":\"SUCCEEDED\"}"));
        server.createContext("/internal/settlements", exchange -> respond(exchange, calls, "{\"status\":\"SUCCEEDED\"}"));
        server.createContext("/internal/vouchers/release", exchange -> respond(exchange, calls, "{\"status\":\"SUCCEEDED\"}"));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            HttpDownstreamGateway gateway = new HttpDownstreamGateway(RestClient.builder(), baseUrl, baseUrl, "contract-token");

            var reservation = gateway.reserve("reservation:create-1", 1L, List.of(new CreateOrderItem(10L, 1)));
            assertThat(reservation.items()).singleElement().satisfies(item -> assertThat(item.productName()).isEqualTo("Phone"));
            assertThat(gateway.quote("quote:create-1", 1L, new BigDecimal("100.00"), 3L).payableAmount())
                    .isEqualByComparingTo("90.00");
            assertThat(gateway.debit("payment:pay-1", 7L, 1L, new BigDecimal("90.00"), "COIN", null))
                    .isEqualTo(RemoteResult.SUCCEEDED);
            assertThat(gateway.paymentResult("payment:pay-1")).isEqualTo(RemoteResult.SUCCEEDED);
            assertThat(gateway.refund("refund:refund-1", "payment:pay-1", 7L, 1L, new BigDecimal("90.00")))
                    .isEqualTo(RemoteResult.SUCCEEDED);
            assertThat(gateway.refundResult("refund:refund-1")).isEqualTo(RemoteResult.SUCCEEDED);
            assertThat(gateway.settle("settlement:receive-1:2", 7L, 2L, new BigDecimal("100.00")))
                    .isEqualTo(RemoteResult.SUCCEEDED);
            assertThat(gateway.settlementResult("settlement:receive-1:2")).isEqualTo(RemoteResult.SUCCEEDED);
            gateway.releaseVoucher("voucher-release:cancel-1", 7L, 3L, 1L);

            assertThat(calls).contains(
                    "POST /internal/inventory/reservations reservation:create-1 contract-token",
                    "POST /internal/checkout/quote quote:create-1 contract-token",
                    "POST /internal/payments/debit payment:pay-1 contract-token",
                    "GET /internal/payments/payment:pay-1 - contract-token",
                    "POST /internal/payments/refund refund:refund-1 contract-token",
                    "GET /internal/payments/refund:refund-1 - contract-token",
                    "POST /internal/settlements settlement:receive-1:2 contract-token",
                    "GET /internal/payments/settlement:receive-1:2 - contract-token",
                    "POST /internal/vouchers/release voucher-release:cancel-1 contract-token");
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, List<String> calls, String body) throws IOException {
        String idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        String internalToken = exchange.getRequestHeaders().getFirst("X-Internal-Service-Token");
        calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
                + (idempotencyKey == null ? "-" : idempotencyKey) + " " + internalToken);
        exchange.getRequestBody().readAllBytes();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
