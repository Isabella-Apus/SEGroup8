package com.segroup8.order;

import com.segroup8.order.ApiModels.*;
import com.segroup8.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@Validated
class OrderController {
    private final OrderService service;

    OrderController(OrderService service) { this.service = service; }

    @PostMapping("/create")
    ApiResponse<PublicOrderView> create(HttpServletRequest http, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(ApiModels.publicOrder(service.create(user(http).userId(), key, request)));
    }

    @GetMapping("/list")
    ApiResponse<PageView<PublicOrderView>> list(HttpServletRequest http,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) Integer orderStatus, @RequestParam(required = false) Integer refundStatus,
            @RequestParam(required = false) String productType, @RequestParam(required = false) String keyword) {
        return ApiResponse.success(ApiModels.publicOrders(service.buyerOrders(user(http).userId(), pageNum, pageSize,
                orderStatus, refundStatus, productType, keyword)));
    }

    @GetMapping("/detail/{id}")
    ApiResponse<PublicOrderView> detail(HttpServletRequest http, @PathVariable long id) {
        return ApiResponse.success(ApiModels.publicOrder(service.getForBuyer(id, user(http).userId())));
    }

    @PostMapping("/{id}/pay")
    ApiResponse<PublicOrderView> pay(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody(required = false) PayRequest request) {
        return ApiResponse.success(ApiModels.publicOrder(service.pay(id, user(http).userId(), key, request)));
    }

    @PostMapping("/{id}/cancel")
    ApiResponse<PublicOrderView> cancel(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.success(ApiModels.publicOrder(service.cancel(id, user(http).userId(), key)));
    }

    @PostMapping("/{id}/ship")
    ApiResponse<PublicOrderView> ship(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody(required = false) ShipRequest request) {
        return ApiResponse.success(ApiModels.publicOrder(service.ship(id, user(http).userId(), key, request)));
    }

    @PostMapping("/{id}/remind-ship")
    ApiResponse<String> remind(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key) {
        service.remindShip(id, user(http).userId(), key);
        return ApiResponse.success("QUEUED");
    }

    @PostMapping("/{id}/confirm-receive")
    ApiResponse<PublicOrderView> receive(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.success(ApiModels.publicOrder(service.confirmReceive(id, user(http).userId(), key)));
    }

    @PostMapping("/{id}/complete")
    ApiResponse<PublicOrderView> complete(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.success(ApiModels.publicOrder(service.complete(id, user(http).userId(), key)));
    }

    @PostMapping("/{id}/refund")
    ApiResponse<PublicOrderView> refund(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody(required = false) RefundRequest request) {
        return ApiResponse.success(ApiModels.publicOrder(service.requestRefund(id, user(http).userId(), key, request)));
    }

    @PostMapping("/{id}/refund/approve")
    ApiResponse<PublicOrderView> approve(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody(required = false) RefundDecision request) {
        JwtPrincipal principal = user(http);
        return ApiResponse.success(ApiModels.publicOrder(service.decideRefund(id, principal.userId(), principal.role(), true, key,
                request == null ? null : request.remark())));
    }

    @PostMapping("/{id}/refund/reject")
    ApiResponse<PublicOrderView> reject(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody(required = false) RefundDecision request) {
        JwtPrincipal principal = user(http);
        return ApiResponse.success(ApiModels.publicOrder(service.decideRefund(id, principal.userId(), principal.role(), false, key,
                request == null ? null : request.remark())));
    }

    @GetMapping("/seller/list")
    ApiResponse<PageView<PublicOrderView>> seller(HttpServletRequest http,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) Integer orderStatus, @RequestParam(required = false) Integer refundStatus,
            @RequestParam(required = false) String productType, @RequestParam(required = false) String keyword) {
        return ApiResponse.success(ApiModels.publicOrders(service.sellerOrders(user(http).userId(), pageNum, pageSize,
                orderStatus, refundStatus, productType, keyword)));
    }

    @GetMapping("/seller/detail/{id}")
    ApiResponse<PublicOrderView> sellerDetail(HttpServletRequest http, @PathVariable long id) {
        return ApiResponse.success(ApiModels.publicOrder(service.getForSeller(id, user(http).userId())));
    }

    @PostMapping("/{id}/review")
    ApiResponse<PublicOrderView> review(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ReviewRequest request) {
        long buyerId = user(http).userId();
        OrderView order = service.getForBuyer(id, buyerId);
        List<ItemReviewRequest> items = order.items().stream()
                .map(item -> new ItemReviewRequest(item.productType(), item.productId(), request.score(), request.content()))
                .toList();
        return ApiResponse.success(ApiModels.publicOrder(service.review(id, buyerId, key, items)));
    }

    @PostMapping("/{id}/review/items")
    ApiResponse<PublicOrderView> reviewItems(HttpServletRequest http, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ItemReviewsRequest request) {
        return ApiResponse.success(ApiModels.publicOrder(service.review(id, user(http).userId(), key, request.items())));
    }

    private JwtPrincipal user(HttpServletRequest request) { return RequestSecurityFilter.principal(request); }
}

@RestController
@RequestMapping("/api/review")
@Validated
class ReviewController {
    private final OrderService service;
    private final OrderRepository repository;

    ReviewController(OrderService service, OrderRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping("/my")
    ApiResponse<PageView<ReviewView>> mine(HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(repository.reviews(RequestSecurityFilter.principal(request).userId(), pageNum, pageSize));
    }

    @GetMapping("/seller/list")
    ApiResponse<PageView<ReviewView>> seller(HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(repository.sellerReviews(RequestSecurityFilter.principal(request).userId(), pageNum, pageSize));
    }

    @PostMapping("/followup")
    ApiResponse<ReviewView> followUp(HttpServletRequest request, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody FollowUpReviewRequest body) {
        return ApiResponse.success(service.followUp(RequestSecurityFilter.principal(request).userId(), key, body));
    }

    @PostMapping({"/{id}/reply", "/seller/{id}/reply"})
    ApiResponse<Void> reply(HttpServletRequest request, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ReviewReplyRequest body) {
        service.replyReview(id, RequestSecurityFilter.principal(request).userId(), key, body.resolvedReply());
        return ApiResponse.success();
    }
}

@RestController
@RequestMapping("/api/logistics")
class LogisticsController {
    private final OrderService service;

    LogisticsController(OrderService service) { this.service = service; }

    @GetMapping({"/{orderId}", "/order/{orderId}/trace"})
    ApiResponse<List<LogisticsView>> traces(HttpServletRequest request, @PathVariable long orderId) {
        return ApiResponse.success(service.logistics(orderId, RequestSecurityFilter.principal(request).userId()));
    }

    @PostMapping("/trace")
    ApiResponse<LogisticsView> push(HttpServletRequest request, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody LogisticsPushRequest body) {
        return ApiResponse.success(service.pushLogistics(body, RequestSecurityFilter.principal(request).userId(), key));
    }

    @PostMapping("/push-next")
    ApiResponse<LogisticsView> pushNext(HttpServletRequest request, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody LogisticsPushRequest body) {
        return ApiResponse.success(service.pushNextLogistics(body.orderId(), RequestSecurityFilter.principal(request).userId(), key));
    }
}

@RestController
@RequestMapping("/api/admin/orders")
@Validated
class AdminOrderController {
    private final OrderService service;
    private final OrderRepository repository;

    AdminOrderController(OrderService service, OrderRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping({"", "/list"})
    ApiResponse<PageView<PublicOrderView>> list(HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        admin(request);
        return ApiResponse.success(ApiModels.publicOrders(repository.listAll(pageNum, pageSize)));
    }

    @GetMapping({"/{id}", "/detail/{id}"})
    ApiResponse<PublicOrderView> detail(HttpServletRequest request, @PathVariable long id) {
        admin(request);
        return ApiResponse.success(ApiModels.publicOrder(repository.get(id)));
    }

    @GetMapping("/{id}/after-sale-logs")
    ApiResponse<List<AfterSaleLogView>> afterSaleLogs(HttpServletRequest request, @PathVariable long id) {
        admin(request);
        return ApiResponse.success(repository.afterSaleLogs(id));
    }

    @PostMapping("/batch-close")
    ApiResponse<Void> close(HttpServletRequest request, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody BatchCloseRequest body) {
        admin(request);
        service.closeAdminBatch(body.orderIds(), key);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/refund/approve")
    ApiResponse<PublicOrderView> approve(HttpServletRequest request, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody(required = false) RefundDecision decision) {
        JwtPrincipal principal = admin(request);
        return ApiResponse.success(ApiModels.publicOrder(service.decideRefund(id, principal.userId(), principal.role(), true, key,
                decision == null ? null : decision.remark())));
    }

    @PostMapping("/{id}/refund/reject")
    ApiResponse<PublicOrderView> reject(HttpServletRequest request, @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key, @RequestBody(required = false) RefundDecision decision) {
        JwtPrincipal principal = admin(request);
        return ApiResponse.success(ApiModels.publicOrder(service.decideRefund(id, principal.userId(), principal.role(), false, key,
                decision == null ? null : decision.remark())));
    }

    private JwtPrincipal admin(HttpServletRequest request) {
        JwtPrincipal principal = RequestSecurityFilter.principal(request);
        if (!"ADMIN".equals(principal.role())) {
            throw new OrderException("ADMIN_REQUIRED", "Administrator role is required", 403);
        }
        return principal;
    }
}

@RestController
@RequestMapping("/internal/orders")
class InternalOrderController {
    private final OrderService service;
    private final OrderRepository repository;

    InternalOrderController(OrderService service, OrderRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping("/secondhand")
    OrderView secondhand(@Valid @RequestBody SecondhandOrderRequest request) { return service.createSecondhand(request); }

    @GetMapping("/by-business-key/{key}")
    OrderView byKey(@PathVariable String key) {
        return repository.byBusinessKey(key)
                .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", "Order does not exist", 404));
    }

    @GetMapping("/{id}/snapshot")
    InternalSnapshot snapshot(@PathVariable long id) { return repository.snapshot(id); }
}

@RestControllerAdvice
class OrderErrorHandler {
    @ExceptionHandler(OrderException.class)
    ResponseEntity<ApiResponse<Void>> order(OrderException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ApiResponse<>(exception.code(), exception.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(new ApiResponse<>("VALIDATION_FAILED", message, null));
    }

    @ExceptionHandler({ServletRequestBindingException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ApiResponse<Void>> badRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("INVALID_REQUEST", "Request headers, parameters or body are invalid", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("INTERNAL_ERROR", "Unexpected order-service error", null));
    }
}
