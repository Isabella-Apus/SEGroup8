package com.segroup8.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.segroup8.platform.annotation.LoginRequired;
import com.segroup8.platform.common.Result;
import com.segroup8.platform.dto.VoucherSaveRequest;
import com.segroup8.platform.service.VoucherService;
import com.segroup8.platform.vo.VoucherVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "卖家分页查询优惠券")
    @GetMapping("/seller/list")
    @LoginRequired
    public Result<IPage<VoucherVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(voucherService.listByShop(page, pageSize));
    }

    @Operation(summary = "用户分页查询可领取优惠券")
    @GetMapping("/list")
    @LoginRequired
    public Result<IPage<VoucherVO>> availableList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(voucherService.pageAvailableForUser(page, pageSize));
    }

    @Operation(summary = "用户领取优惠券")
    @PostMapping("/{id}/claim")
    @LoginRequired
    public Result<Void> claim(@PathVariable Long id) {
        voucherService.claim(id);
        return Result.success(null);
    }

    @Operation(summary = "我的优惠券")
    @GetMapping("/my")
    @LoginRequired
    public Result<IPage<VoucherVO>> myVouchers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(voucherService.pageMine(page, pageSize));
    }

    @Operation(summary = "结算可选优惠券")
    @GetMapping("/my/available")
    @LoginRequired
    public Result<IPage<VoucherVO>> myAvailableVouchers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String shopIds,
            @RequestParam(required = false) BigDecimal totalAmount) {
        return Result.success(voucherService.pageMineAvailableForCheckout(page, pageSize, shopIds, totalAmount));
    }

    @Operation(summary = "结算不可用券原因")
    @GetMapping("/my/available/reasons")
    @LoginRequired
    public Result<List<String>> myUnavailableVoucherReasons(
            @RequestParam(required = false) String shopIds,
            @RequestParam(required = false) BigDecimal totalAmount) {
        return Result.success(voucherService.checkoutUnavailableReasons(shopIds, totalAmount));
    }

    @Operation(summary = "卖家创建优惠券")
    @PostMapping("/seller")
    @LoginRequired
    public Result<VoucherVO> create(@Valid @RequestBody VoucherSaveRequest request) {
        return Result.success(voucherService.create(request));
    }

    @Operation(summary = "卖家更新优惠券")
    @PutMapping("/seller/{id}")
    @LoginRequired
    public Result<VoucherVO> update(
            @PathVariable Long id,
            @Valid @RequestBody VoucherSaveRequest request) {
        return Result.success(voucherService.update(id, request));
    }

    @Operation(summary = "卖家关闭优惠券")
    @PostMapping("/seller/{id}/close")
    @LoginRequired
    public Result<Void> close(@PathVariable Long id) {
        voucherService.close(id);
        return Result.success(null);
    }

    @Operation(summary = "卖家删除优惠券")
    @DeleteMapping("/seller/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return Result.success(null);
    }

    @Operation(summary = "管理员分页查询优惠券")
    @GetMapping("/admin/list")
    @LoginRequired
    public Result<IPage<VoucherVO>> adminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer scopeType) {
        return Result.success(voucherService.listForAdmin(page, pageSize, name, status, scopeType));
    }

    @Operation(summary = "管理员创建优惠券")
    @PostMapping("/admin")
    @LoginRequired
    public Result<VoucherVO> adminCreate(@Valid @RequestBody VoucherSaveRequest request) {
        return Result.success(voucherService.createForAdmin(request));
    }

    @Operation(summary = "管理员更新优惠券")
    @PutMapping("/admin/{id}")
    @LoginRequired
    public Result<VoucherVO> adminUpdate(
            @PathVariable Long id,
            @Valid @RequestBody VoucherSaveRequest request) {
        return Result.success(voucherService.updateForAdmin(id, request));
    }

    @Operation(summary = "管理员关闭优惠券")
    @PostMapping("/admin/{id}/close")
    @LoginRequired
    public Result<Void> adminClose(@PathVariable Long id) {
        voucherService.closeForAdmin(id);
        return Result.success(null);
    }

    @Operation(summary = "管理员删除优惠券")
    @DeleteMapping("/admin/{id}")
    @LoginRequired
    public Result<Void> adminDelete(@PathVariable Long id) {
        voucherService.deleteForAdmin(id);
        return Result.success(null);
    }
}
