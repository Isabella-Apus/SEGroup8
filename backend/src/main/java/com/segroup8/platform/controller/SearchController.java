package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.service.SearchBehaviorService;
import com.segroup8.platform.vo.SearchHotKeywordVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchBehaviorService searchBehaviorService;

    public SearchController(SearchBehaviorService searchBehaviorService) {
        this.searchBehaviorService = searchBehaviorService;
    }

    @Operation(summary = "获取我的搜索历史")
    @GetMapping("/history")
    public Result<List<String>> history() {
        return Result.success(searchBehaviorService.getMyHistory());
    }

    @Operation(summary = "获取热门搜索")
    @GetMapping("/hot")
    public Result<List<SearchHotKeywordVO>> hot() {
        return Result.success(searchBehaviorService.getHotKeywords());
    }
}
