package com.segroup8.finance;

import com.segroup8.finance.ApiModels.PaymentResult;
import com.segroup8.finance.ApiModels.Recharge;
import com.segroup8.finance.ApiModels.TransactionView;
import com.segroup8.finance.ApiModels.Wallet;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
class FinanceController {
    private final FinanceService service;
    private final IdempotencyKeyService idempotency;

    FinanceController(FinanceService service, IdempotencyKeyService idempotency) {
        this.service = service;
        this.idempotency = idempotency;
    }

    @Operation(summary="UC23 钱包与经营账户看板")
    @GetMapping("/dashboard")
    Wallet dashboard() {
        return service.wallet(RequestContext.requireUser().userId());
    }

    @Operation(summary="UC23 幂等模拟充值")
    @PostMapping("/recharge")
    PaymentResult recharge(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody Recharge request) {
        long userId = RequestContext.requireUser().userId();
        return idempotency.execute(scope("POST /api/finance/recharge", userId), idempotencyKey, request,
                PaymentResult.class, () -> service.recharge(userId, request.requestId(), request.amount(), request.channel()));
    }

    @Operation(summary="UC23 个人钱包流水")
    @GetMapping("/my-wallet/records")
    List<TransactionView> walletRecords() {
        return service.records(RequestContext.requireUser().userId(), "PERSONAL");
    }

    @Operation(summary="UC23 商家经营流水")
    @GetMapping("/business/records")
    List<TransactionView> businessRecords() {
        return service.records(RequestContext.requireRole("OFFICIAL_SELLER").userId(), "BUSINESS");
    }

    private static String scope(String operation, long userId) { return operation + ":user:" + userId; }
}
