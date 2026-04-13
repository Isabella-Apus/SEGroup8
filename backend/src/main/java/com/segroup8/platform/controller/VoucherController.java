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
}