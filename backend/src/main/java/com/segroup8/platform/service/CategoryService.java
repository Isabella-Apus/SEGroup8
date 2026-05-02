package com.segroup8.platform.service;

import com.segroup8.platform.vo.CategoryTreeNodeVO;

import java.util.List;
import java.util.Set;

public interface CategoryService {

    int FOOD_MAIN_CATEGORY_ID = 7;

    List<CategoryTreeNodeVO> getCategoryTree(boolean excludeFood);

    boolean isMainCategory(Integer categoryId);

    boolean isSubCategoryOf(Integer mainCategoryId, Integer subCategoryId);

    Set<Integer> resolveLeafCategoryIds(Integer categoryId);

    String getCategoryName(Integer categoryId);
}
