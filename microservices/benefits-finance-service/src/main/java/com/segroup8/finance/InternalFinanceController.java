package com.segroup8.finance;

import com.segroup8.finance.ApiModels.DebitRequest;
import com.segroup8.finance.ApiModels.PaymentResult;
import com.segroup8.finance.ApiModels.QuoteRequest;
import com.segroup8.finance.ApiModels.QuoteResult;
import com.segroup8.finance.ApiModels.RefundRequest;
import com.segroup8.finance.ApiModels.SettlementRequest;
import com.segroup8.finance.ApiModels.VoucherAction;
import com.segroup8.finance.ApiModels.VoucherActionResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
class InternalFinanceController {
    private final VoucherService vouchers;
    private final FinanceService finance;
    private final IdempotencyKeyService idempotency;

    InternalFinanceController(VoucherService vouchers, FinanceService finance, IdempotencyKeyService idempotency) {
        this.vouchers = vouchers;
        this.finance = finance;
        this.idempotency = idempotency;
    }

    @PostMapping("/checkout/quote")
    QuoteResult quote(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody QuoteRequest request) {
        return idempotency.execute("POST /internal/checkout/quote", idempotencyKey, request, QuoteResult.class,
                () -> vouchers.quote(request));
    }

    @PostMapping("/vouchers/reserve")
    VoucherActionResult reserve(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody VoucherAction request) {
        return idempotency.execute("POST /internal/vouchers/reserve", idempotencyKey, request,
                VoucherActionResult.class, () -> vouchers.reserve(request));
    }

    @PostMapping("/vouchers/consume")
    VoucherActionResult consume(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody VoucherAction request) {
        return idempotency.execute("POST /internal/vouchers/consume", idempotencyKey, request,
                VoucherActionResult.class, () -> vouchers.consume(request));
    }

    @PostMapping("/vouchers/release")
    VoucherActionResult release(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody VoucherAction request) {
        return idempotency.execute("POST /internal/vouchers/release", idempotencyKey, request,
                VoucherActionResult.class, () -> vouchers.release(request));
    }

    @PostMapping("/payments/debit")
    PaymentResult debit(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody DebitRequest request) {
        return idempotency.execute("POST /internal/payments/debit", idempotencyKey, request, PaymentResult.class,
                () -> finance.debit(request));
    }

    @GetMapping("/payments/{paymentRequestId}")
    PaymentResult payment(@PathVariable String paymentRequestId) { return finance.payment(paymentRequestId); }

    @PostMapping("/payments/refund")
    PaymentResult refund(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody RefundRequest request) {
        return idempotency.execute("POST /internal/payments/refund", idempotencyKey, request, PaymentResult.class,
                () -> finance.refund(request));
    }

    @PostMapping("/settlements")
    PaymentResult settlement(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody SettlementRequest request) {
        return idempotency.execute("POST /internal/settlements", idempotencyKey, request, PaymentResult.class,
                () -> finance.settlement(request));
    }
}
