package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.service.BrowseHistoryService;
import com.segroup8.platform.vo.BrowseHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    public BrowseHistoryController(BrowseHistoryService browseHistoryService) {
        this.browseHistoryService = browseHistoryService;
    }

    @Operation(summary = "获取当前用户浏览记录")
    @GetMapping("/browse-history")
    public Result<List<BrowseHistoryVO>> browseHistory() {
        return Result.success(browseHistoryService.getBrowseHistory());
    }

    @Operation(summary = "删除单条浏览记录")
    @DeleteMapping("/browse-history/{historyId}")
    public Result<Void> deleteOne(@PathVariable Long historyId) {
        browseHistoryService.deleteBrowseHistory(historyId);
        return Result.success();
    }

    @Operation(summary = "批量删除浏览记录")
    @PostMapping("/browse-history/delete-batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> historyIds) {
        browseHistoryService.deleteBrowseHistoryBatch(historyIds);
        return Result.success();
    }

    @Operation(summary = "清空浏览记录")
    @DeleteMapping("/browse-history/all")
    public Result<Void> clearAll() {
        browseHistoryService.clearBrowseHistory();
        return Result.success();
    }
}
