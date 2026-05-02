package com.segroup8.platform.controller;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.service.CategoryService;
import com.segroup8.platform.vo.CategoryTreeNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类树")
    @GetMapping("/tree")
    public Result<List<CategoryTreeNodeVO>> tree(@RequestParam(defaultValue = "NEW") String scene) {
        boolean excludeFood = "SECONDHAND".equalsIgnoreCase(scene);
        return Result.success(categoryService.getCategoryTree(excludeFood));
    }
}
