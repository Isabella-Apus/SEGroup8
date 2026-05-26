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

    private static final Map<Integer, String> CATEGORY_NAME_BY_ID = Map.ofEntries(
            Map.entry(1, "电子数码"),
            Map.entry(2, "服饰鞋包"),
            Map.entry(3, "家居生活"),
            Map.entry(4, "美妆个护"),
            Map.entry(5, "运动户外"),
            Map.entry(6, "图书音像"),
            Map.entry(7, "美食"),
            Map.entry(8, "其他"),
            Map.entry(101, "手机"),
            Map.entry(102, "电脑/平板"),
            Map.entry(103, "摄影摄像"),
            Map.entry(104, "影音娱乐"),
            Map.entry(105, "智能穿戴"),
            Map.entry(201, "女装"),
            Map.entry(202, "男装"),
            Map.entry(203, "运动服饰"),
            Map.entry(204, "鞋包"),
            Map.entry(205, "配饰"),
            Map.entry(301, "家具家装"),
            Map.entry(302, "厨房用具"),
            Map.entry(303, "居家日用"),
            Map.entry(304, "家用电器"),
            Map.entry(305, "收纳整理"),
            Map.entry(401, "面部护肤"),
            Map.entry(402, "彩妆"),
            Map.entry(403, "个人护理"),
            Map.entry(404, "香水香氛"),
            Map.entry(405, "美容仪器"),
            Map.entry(501, "健身器材"),
            Map.entry(502, "户外装备"),
            Map.entry(503, "体育用品"),
            Map.entry(504, "骑行运动"),
            Map.entry(601, "教材教辅"),
            Map.entry(602, "小说文学"),
            Map.entry(603, "艺术收藏"),
            Map.entry(604, "办公文具"),
            Map.entry(701, "休闲零食"),
            Map.entry(702, "粮油调味"),
            Map.entry(703, "生鲜果蔬"),
            Map.entry(704, "冲调饮品"),
            Map.entry(705, "地方特产"),
            Map.entry(801, "未分类"));

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
        return category == null ? CATEGORY_NAME_BY_ID.get(categoryId) : displayName(category);
    }

    private List<Category> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1));
    }

    private CategoryTreeNodeVO toNode(Category category) {
        CategoryTreeNodeVO node = new CategoryTreeNodeVO();
        node.setId(category.getId());
        node.setName(displayName(category));
        node.setParentId(category.getParentId());
        return node;
    }

    private String displayName(Category category) {
        if (category == null) {
            return null;
        }
        return CATEGORY_NAME_BY_ID.getOrDefault(category.getId(), category.getName());
    }
}
