package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.service.CreditService;
import com.segroup8.platform.vo.CreditScoreVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "信用评分", description = "查询买家/卖家信用分及变动记录")
@RestController
@RequestMapping("/api/credit")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    /**
     * 获取当前登录用户自己的信用信息
     * GET /api/credit/me
     */
    @Operation(summary = "获取我的信用信息（买家+卖家）")
    @GetMapping("/me")
    public Result<CreditScoreVO> myCredit() {
        return Result.success(creditService.getMyCredit());
    }

    /**
     * 查看指定用户的信用信息（买家查卖家，或卖家查买家）
     * GET /api/credit/{userId}
     */
    @Operation(summary = "查看指定用户的信用信息")
    @GetMapping("/{userId}")
    public Result<CreditScoreVO> userCredit(@PathVariable Long userId) {
        return Result.success(creditService.getCreditInfo(userId));
    }
}