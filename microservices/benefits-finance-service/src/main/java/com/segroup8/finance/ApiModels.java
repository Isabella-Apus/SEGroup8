package com.segroup8.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}

    public record VoucherSave(
            @NotBlank @Size(max=100) String name,
            @NotBlank @Pattern(regexp="AMOUNT|RATE") String discountType,
            @DecimalMin(value="0.01") BigDecimal discountAmount,
            @DecimalMin(value="0.0001") BigDecimal discountRate,
            @NotNull @DecimalMin("0.00") BigDecimal minAmount,
            @NotNull @Min(1) Integer totalCount,
            @NotNull Instant startTime,
            @NotNull @Future Instant endTime,
            @Positive Long shopId,
            @Positive Long productId,
            String scopeType) {}

    public record VoucherView(long id, String issuerType, String voucherType, Long issuerUserId,
            String scopeType, Long shopId, Long productId, String name, String discountType,
            BigDecimal discountAmount, BigDecimal discountRate, BigDecimal minAmount, int totalCount,
            int receivedCount, int usedCount, Instant startTime, Instant endTime, String status) {}

    public record ClaimResult(long userVoucherId, long voucherId, String status, Instant expiresAt) {}
    public record Wallet(BigDecimal personalBalance, BigDecimal businessBalance, String currency, int version) {}
    public record Recharge(@NotBlank @Size(max=48) @Pattern(regexp="[A-Za-z0-9._:-]+") String requestId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp="WECHAT|ALIPAY|BANK") String channel) {}
    public record TransactionView(String transactionId, Long orderId, long userId, String accountType,
            String tradeType, BigDecimal amount, BigDecimal balanceAfter, String currency,
            String reversalOf, String remark, Instant createdAt) {}

    public record QuoteRequest(@NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._:-]+") String orderRequestId,
            @NotNull @Positive Long userId, @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Positive Long voucherId, List<@Positive Long> shopIds, List<@Positive Long> productIds) {}
    public record QuoteResult(String quoteId, int quoteVersion, BigDecimal originalAmount,
            BigDecimal discountAmount, BigDecimal payableAmount, String currency, Instant expiresAt,
            Long voucherId) {}
    public record VoucherAction(@NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._:-]+") String orderRequestId,
            @NotNull @Positive Long userId, @NotNull @Positive Long voucherId, @Positive Long orderId) {}
    public record VoucherActionResult(long voucherId, String orderRequestId, String status) {}
    public record DebitRequest(
            @NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._:-]+") String paymentRequestId,
            @NotNull @Positive Long orderId, @NotNull @Positive Long userId,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {}
    public record RefundRequest(
            @NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._:-]+") String refundRequestId,
            @NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._:-]+") String paymentRequestId,
            @NotNull @Positive Long orderId, @NotNull @Positive Long userId,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {}
    public record SettlementRequest(@NotNull @Positive Long orderId, @NotNull @Positive Long sellerId,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {}
    public record PaymentResult(String requestId, String requestType, Long orderId, Long userId,
            Long sellerId, BigDecimal amount, String currency, String status, String transactionId,
            String originalRequestId, Instant completedAt) {}
    public record ErrorResponse(String code, String message, String requestId, Instant timestamp) {}
}
