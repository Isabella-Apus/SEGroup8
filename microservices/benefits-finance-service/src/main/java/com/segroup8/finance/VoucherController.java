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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voucher")
class VoucherController {
    private final VoucherService service;

    VoucherController(VoucherService service) { this.service = service; }

    @Operation(summary="UC21 卖家优惠券列表")
    @GetMapping("/seller/list")
    List<VoucherView> sellerList() {
        return service.sellerList(RequestContext.requireRole("OFFICIAL_SELLER").userId());
    }

    @Operation(summary="UC21 卖家创建优惠券")
    @PostMapping("/seller")
    @ResponseStatus(HttpStatus.CREATED)
    VoucherView sellerCreate(@Valid @RequestBody VoucherSave request) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        return service.create(request, "SELLER", userId);
    }

    @Operation(summary="UC21 卖家更新优惠券")
    @PutMapping("/seller/{id}")
    VoucherView sellerUpdate(@PathVariable long id, @Valid @RequestBody VoucherSave request) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        return service.update(id, request, "SELLER", userId);
    }

    @Operation(summary="UC21 卖家关闭优惠券")
    @PostMapping("/seller/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sellerClose(@PathVariable long id) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        service.close(id, "SELLER", userId);
    }

    @Operation(summary="UC21 卖家删除未领取优惠券")
    @DeleteMapping("/seller/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sellerDelete(@PathVariable long id) {
        long userId = RequestContext.requireRole("OFFICIAL_SELLER").userId();
        service.delete(id, "SELLER", userId);
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
    VoucherView adminCreate(@Valid @RequestBody VoucherSave request) {
        long userId = RequestContext.requireRole("ADMIN").userId();
        return service.create(request, "ADMIN", userId);
    }

    @Operation(summary="UC21 管理员更新平台优惠券")
    @PutMapping("/admin/{id}")
    VoucherView adminUpdate(@PathVariable long id, @Valid @RequestBody VoucherSave request) {
        RequestContext.requireRole("ADMIN");
        return service.update(id, request, "ADMIN", null);
    }

    @Operation(summary="UC21 管理员关闭优惠券")
    @PostMapping("/admin/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void adminClose(@PathVariable long id) {
        RequestContext.requireRole("ADMIN");
        service.close(id, "ADMIN", null);
    }

    @Operation(summary="UC21 管理员删除未领取优惠券")
    @DeleteMapping("/admin/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void adminDelete(@PathVariable long id) {
        RequestContext.requireRole("ADMIN");
        service.delete(id, "ADMIN", null);
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
    ClaimResult claim(@PathVariable long id) {
        return service.claim(id, RequestContext.requireUser().userId());
    }

    @Operation(summary="UC22 我的优惠券")
    @GetMapping("/my")
    List<VoucherView> mine() {
        return service.mine(RequestContext.requireUser().userId());
    }
}
