package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.entity.Category;
import com.segroup8.platform.mapper.CategoryMapper;
import com.segroup8.platform.service.CategoryService;
import com.segroup8.platform.vo.CategoryTreeNodeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryTreeNodeVO> getCategoryTree(boolean excludeFood) {
        List<Category> all = listEnabledCategories();
        Map<Integer, List<Category>> groupByParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0 : c.getParentId()));
        List<Category> roots = new ArrayList<>(groupByParent.getOrDefault(0, List.of()));
        roots.sort(Comparator.comparing(Category::getSortOrder).thenComparing(Category::getId));

        List<CategoryTreeNodeVO> result = new ArrayList<>();
        for (Category root : roots) {
            if (excludeFood && root.getId() == FOOD_MAIN_CATEGORY_ID) {
                continue;
            }
            CategoryTreeNodeVO node = toNode(root);
            List<Category> children = new ArrayList<>(groupByParent.getOrDefault(root.getId(), List.of()));
            children.sort(Comparator.comparing(Category::getSortOrder).thenComparing(Category::getId));
            node.setChildren(children.stream().map(this::toNode).toList());
            result.add(node);
        }
        return result;
    }

    @Override
    public boolean isMainCategory(Integer categoryId) {
        if (categoryId == null) {
            return false;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category != null && category.getStatus() != null && category.getStatus() == 1
                && category.getParentId() == null;
    }

    @Override
    public boolean isSubCategoryOf(Integer mainCategoryId, Integer subCategoryId) {
        if (mainCategoryId == null || subCategoryId == null) {
            return false;
        }
        Category sub = categoryMapper.selectById(subCategoryId);
        return sub != null && sub.getStatus() != null && sub.getStatus() == 1
                && sub.getParentId() != null && sub.getParentId().equals(mainCategoryId);
    }

    @Override
    public Set<Integer> resolveLeafCategoryIds(Integer categoryId) {
        if (categoryId == null) {
            return Set.of();
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            return Set.of();
        }
        if (category.getParentId() != null) {
            return Set.of(categoryId);
        }
        List<Category> children = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryId)
                .eq(Category::getStatus, 1));
        if (children.isEmpty()) {
            return Set.of(categoryId);
        }
        return children.stream().map(Category::getId).collect(Collectors.toSet());
    }

    @Override
    public String getCategoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    private List<Category> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1));
    }

    private CategoryTreeNodeVO toNode(Category category) {
        CategoryTreeNodeVO node = new CategoryTreeNodeVO();
        node.setId(category.getId());
        node.setName(category.getName());
        node.setParentId(category.getParentId());
        return node;
    }
}
