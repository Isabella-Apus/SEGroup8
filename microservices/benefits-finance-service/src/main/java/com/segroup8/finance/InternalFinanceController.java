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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
class InternalFinanceController {
    private final VoucherService vouchers;
    private final FinanceService finance;

    InternalFinanceController(VoucherService vouchers, FinanceService finance) {
        this.vouchers = vouchers;
        this.finance = finance;
    }

    @PostMapping("/checkout/quote")
    QuoteResult quote(@Valid @RequestBody QuoteRequest request) { return vouchers.quote(request); }

    @PostMapping("/vouchers/reserve")
    VoucherActionResult reserve(@Valid @RequestBody VoucherAction request) { return vouchers.reserve(request); }

    @PostMapping("/vouchers/consume")
    VoucherActionResult consume(@Valid @RequestBody VoucherAction request) { return vouchers.consume(request); }

    @PostMapping("/vouchers/release")
    VoucherActionResult release(@Valid @RequestBody VoucherAction request) { return vouchers.release(request); }

    @PostMapping("/payments/debit")
    PaymentResult debit(@Valid @RequestBody DebitRequest request) { return finance.debit(request); }

    @GetMapping("/payments/{paymentRequestId}")
    PaymentResult payment(@PathVariable String paymentRequestId) { return finance.payment(paymentRequestId); }

    @PostMapping("/payments/refund")
    PaymentResult refund(@Valid @RequestBody RefundRequest request) { return finance.refund(request); }

    @PostMapping("/settlements")
    PaymentResult settlement(@Valid @RequestBody SettlementRequest request) { return finance.settlement(request); }
}
