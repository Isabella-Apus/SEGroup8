package com.segroup8.secondhand.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.segroup8.secondhand.client.HttpOrderGateway;
import com.segroup8.secondhand.domain.TradeOrderRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Tag("DOMAIN_D")
@Tag("CONTRACT")
class HttpOrderGatewayContractTest {
    @Test
    void sendsIdempotentBusinessContractAndParsesPendingPaymentOrder() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://order.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpOrderGateway gateway = new HttpOrderGateway(builder.build(), "internal-test-token",
                (userId, addressId, requestId) -> { throw new AssertionError("persisted snapshot must be reused"); });
        server.expect(once(), requestTo("http://order.test/internal/orders/secondhand"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Service-Token", "internal-test-token"))
                .andExpect(header("X-Request-Id", "SECONDHAND:BARGAIN:88"))
                .andExpect(header("Idempotency-Key", "SECONDHAND:BARGAIN:88"))
                .andExpect(header("X-Idempotency-Key", "SECONDHAND:BARGAIN:88"))
                .andExpect(jsonPath("$.tradeType").value("BARGAIN"))
                .andExpect(jsonPath("$.tradeId").value("88"))
                .andExpect(jsonPath("$.orderBusinessKey").value("SECONDHAND:BARGAIN:88"))
                .andExpect(jsonPath("$.buyerUserId").value(20))
                .andExpect(jsonPath("$.sellerUserId").value(10))
                .andExpect(jsonPath("$.productName").value("Used book"))
                .andExpect(jsonPath("$.receiverName").value("Buyer"))
                .andExpect(jsonPath("$.receiverPhone").value("13800008000"))
                .andExpect(jsonPath("$.receiverProvince").value("Zhejiang"))
                .andExpect(jsonPath("$.receiverCity").value("Hangzhou"))
                .andExpect(jsonPath("$.receiverDetailAddress").value("West Lake Road 1"))
                .andRespond(withSuccess("{\"code\":0,\"message\":\"success\",\"data\":{"
                        + "\"orderId\":901,\"orderNo\":\"ORD901\",\"status\":\"PENDING_PAY\"}}",
                        MediaType.APPLICATION_JSON));

        var receipt = gateway.createSecondhandOrder(request());
        assertThat(receipt.orderId()).isEqualTo(901);
        assertThat(receipt.status()).isEqualTo("PENDING_PAY");
        server.verify();
    }

    @Test
    void looksUpUncertainOrderByBusinessKeyWithInternalAuthentication() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://order.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpOrderGateway gateway = new HttpOrderGateway(builder.build(), "internal-test-token",
                (userId, addressId, requestId) -> { throw new AssertionError("address lookup not expected"); });
        server.expect(once(), requestTo(
                        "http://order.test/internal/orders/by-business-key/SECONDHAND%3ABARGAIN%3A88"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "internal-test-token"))
                .andExpect(header("X-Request-Id", "SECONDHAND:BARGAIN:88"))
                .andRespond(withSuccess("{\"code\":0,\"message\":\"success\",\"data\":{"
                                + "\"orderId\":901,\"orderNo\":\"ORD901\",\"status\":\"PENDING_PAY\"}}",
                        MediaType.APPLICATION_JSON));

        var receipt = gateway.findByBusinessKey("SECONDHAND:BARGAIN:88");
        assertThat(receipt).isPresent();
        assertThat(receipt.orElseThrow().orderId()).isEqualTo(901);
        server.verify();
    }

    private TradeOrderRequest request() {
        LocalDateTime now = LocalDateTime.now();
        return new TradeOrderRequest(1, "BARGAIN", "88", "SECONDHAND:BARGAIN:88", 7, 20, 10,
                new BigDecimal("75.00"), 3L, "Used book", "Buyer", "13800008000", "Zhejiang",
                "Hangzhou", "West Lake Road 1", "议价订单", "PENDING", null, null, null,
                0, null, now, 0, now, now);
    }
}
