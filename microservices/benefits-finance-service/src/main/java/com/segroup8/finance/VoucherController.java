package com.segroup8.finance;

import com.segroup8.finance.ApiModels.ClaimResult;
import com.segroup8.finance.ApiModels.VoucherSave;
import com.segroup8.finance.ApiModels.VoucherView;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voucher")
class VoucherController {
    private final VoucherService service;
    private final IdempotencyKeyService idempotency;

    VoucherController(VoucherService service, IdempotencyKeyService idempotency) {
        this.service = service;
        this.idempotency = idempotency;
    }

    @Operation(summary="UC21 卖家优惠券列表")
    @GetMapping("/seller/list")
    List<VoucherView> sellerList() {
        return service.sellerList(RequestContext.requireRole("OFFICIAL_SELLER").userId());
    }

    @Operation(summary="UC21 卖家创建优惠券")
    @PostMapping("/seller")
    @ResponseStatus(HttpStatus.CREATED)
    VoucherView sellerCreate(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody VoucherSave request) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        return idempotency.execute(scope("POST /api/voucher/seller", userId), idempotencyKey, request,
                VoucherView.class, () -> service.create(request, "SELLER", userId));
    }

    @Operation(summary="UC21 卖家更新优惠券")
    @PutMapping("/seller/{id}")
    VoucherView sellerUpdate(@PathVariable long id, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody VoucherSave request) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        return idempotency.execute(scope("PUT /api/voucher/seller/" + id, userId), idempotencyKey, request,
                VoucherView.class, () -> service.update(id, request, "SELLER", userId));
    }

    @Operation(summary="UC21 卖家关闭优惠券")
    @PostMapping("/seller/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sellerClose(@PathVariable long id, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        idempotency.executeVoid(scope("POST /api/voucher/seller/" + id + "/close", userId), idempotencyKey,
                java.util.Map.of("voucherId", id), () -> service.close(id, "SELLER", userId));
    }

    @Operation(summary="UC21 卖家删除未领取优惠券")
    @DeleteMapping("/seller/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sellerDelete(@PathVariable long id, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        idempotency.executeVoid(scope("DELETE /api/voucher/seller/" + id, userId), idempotencyKey,
                java.util.Map.of("voucherId", id), () -> service.delete(id, "SELLER", userId));
    }

    @Operation(summary="UC21 管理员优惠券列表")
    @GetMapping("/admin/list")
    List<VoucherView> adminList() {
        RequestContext.requireRole("ADMIN");
        return service.adminList();
    }

    @Operation(summary="UC21 管理员创建平台优惠券")
    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    VoucherView adminCreate(@RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody VoucherSave request) {
        long userId = RequestContext.requireRole("ADMIN").userId();
        return idempotency.execute(scope("POST /api/voucher/admin", userId), idempotencyKey, request,
                VoucherView.class, () -> service.create(request, "ADMIN", userId));
    }

    @Operation(summary="UC21 管理员更新平台优惠券")
    @PutMapping("/admin/{id}")
    VoucherView adminUpdate(@PathVariable long id, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey,
            @Valid @RequestBody VoucherSave request) {
        long userId = RequestContext.requireRole("ADMIN").userId();
        return idempotency.execute(scope("PUT /api/voucher/admin/" + id, userId), idempotencyKey, request,
                VoucherView.class, () -> service.update(id, request, "ADMIN", null));
    }

    @Operation(summary="UC21 管理员关闭优惠券")
    @PostMapping("/admin/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void adminClose(@PathVariable long id, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        long userId = RequestContext.requireRole("ADMIN").userId();
        idempotency.executeVoid(scope("POST /api/voucher/admin/" + id + "/close", userId), idempotencyKey,
                java.util.Map.of("voucherId", id), () -> service.close(id, "ADMIN", null));
    }

    @Operation(summary="UC21 管理员删除未领取优惠券")
    @DeleteMapping("/admin/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void adminDelete(@PathVariable long id, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        long userId = RequestContext.requireRole("ADMIN").userId();
        idempotency.executeVoid(scope("DELETE /api/voucher/admin/" + id, userId), idempotencyKey,
                java.util.Map.of("voucherId", id), () -> service.delete(id, "ADMIN", null));
    }

    @Operation(summary="UC22 查询可领取优惠券")
    @GetMapping("/list")
    List<VoucherView> available() {
        RequestContext.requireUser();
        return service.available();
    }

    @Operation(summary="UC22 领取优惠券")
    @PostMapping("/{id}/claim")
    @ResponseStatus(HttpStatus.CREATED)
    ClaimResult claim(@PathVariable long id, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        long userId = RequestContext.requireUser().userId();
        return idempotency.execute(scope("POST /api/voucher/" + id + "/claim", userId), idempotencyKey,
                java.util.Map.of("voucherId", id), ClaimResult.class, () -> service.claim(id, userId));
    }

    @Operation(summary="UC22 我的优惠券")
    @GetMapping("/my")
    List<VoucherView> mine() {
        return service.mine(RequestContext.requireUser().userId());
    }

    private static String scope(String operation, long userId) { return operation + ":user:" + userId; }
}
