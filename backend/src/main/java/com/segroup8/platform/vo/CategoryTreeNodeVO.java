package com.segroup8.platform.vo;

import java.util.ArrayList;
import java.util.List;

public class CategoryTreeNodeVO {

    private Integer id;
    private String name;
    private Integer parentId;
    private List<CategoryTreeNodeVO> children = new ArrayList<>();

    /**
     * 兼容前端级联选择器常用字段命名（value/label）。
     * 保留 id/name 作为后端更直观的字段。
     */
    public Integer getValue() {
        return id;
    }

    public String getLabel() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public List<CategoryTreeNodeVO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTreeNodeVO> children) {
        this.children = children;
    }
}
