package com.segroup8.secondhand.api;

import com.segroup8.secondhand.common.ApiResponse;
import com.segroup8.secondhand.security.InternalServiceAuthenticator;
import com.segroup8.secondhand.service.OrderStatusProjectionService;
import com.segroup8.secondhand.service.ProductApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
public class InternalEventController {
    private final InternalServiceAuthenticator authenticator;
    private final OrderStatusProjectionService orderStatuses;
    private final ProductApplicationService products;

    public InternalEventController(InternalServiceAuthenticator authenticator,
            OrderStatusProjectionService orderStatuses, ProductApplicationService products) {
        this.authenticator = authenticator;
        this.orderStatuses = orderStatuses;
        this.products = products;
    }

    @PostMapping("/order-status-changed")
    ApiResponse<EventConsumeView> orderStatus(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String token,
            @Valid @RequestBody OrderStatusChangedEvent event) {
        authenticator.verify(token);
        boolean consumed = orderStatuses.consume(event.eventId(), event.orderBusinessKey(),
                event.orderId(), event.newStatus());
        return ApiResponse.success(new EventConsumeView(consumed ? "CONSUMED" : "DUPLICATE"));
    }

    @PostMapping("/product-risk-decided")
    ApiResponse<EventConsumeView> riskDecision(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String token,
            @Valid @RequestBody ProductRiskDecidedEvent event) {
        authenticator.verify(token);
        boolean consumed = products.applyRiskDecision(event.eventId(), event.productId(), event.decision());
        return ApiResponse.success(new EventConsumeView(consumed ? "CONSUMED" : "DUPLICATE"));
    }

    public record OrderStatusChangedEvent(@NotBlank String eventId, @NotBlank String orderBusinessKey,
            @NotNull Long orderId, @NotBlank String newStatus) {
    }

    public record ProductRiskDecidedEvent(@NotBlank String eventId, @NotNull Long productId,
            @NotBlank String decision) {
    }

    public record EventConsumeView(String status) {
    }
}
