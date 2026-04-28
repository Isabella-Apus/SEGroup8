package com.segroup8.platform.vo;

import java.util.ArrayList;
import java.util.List;

public class CategoryTreeNodeVO {

    private Integer id;
    private String name;
    private Integer parentId;
    private List<CategoryTreeNodeVO> children = new ArrayList<>();

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
