package com.segroup8.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class SecondhandProductSaveRequest {

    @NotBlank(message = "二手商品名称不能为空")
    @Size(max = 120, message = "二手商品名称长度不能超过120")
    private String name;

    @Size(max = 255, message = "封面地址长度不能超过255")
    private String cover;

    @NotEmpty(message = "请至少上传一张商品图片")
    @Size(max = 9, message = "商品图片不能超过9张")
    private List<@NotBlank(message = "商品图片地址不能为空")
            @Size(max = 255, message = "商品图片地址长度不能超过255") String> images;

    @Size(max = 2000, message = "商品描述长度不能超过2000")
    private String description;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.01", message = "原价必须大于0")
    @Digits(integer = 8, fraction = 2, message = "原价格式不正确")
    private BigDecimal originPrice;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于0")
    @Digits(integer = 8, fraction = 2, message = "售价格式不正确")
    private BigDecimal salePrice;

    @NotNull(message = "一级分类不能为空")
    private Integer categoryId;

    @NotNull(message = "二级分类不能为空")
    private Integer subCategoryId;

    @NotBlank(message = "成色不能为空")
    @Size(max = 30, message = "成色长度不能超过30")
    private String conditionLevel;

    @NotNull(message = "是否可议价不能为空")
    @Min(value = 0, message = "是否可议价只能为0或1")
    @Max(value = 1, message = "是否可议价只能为0或1")
    private Integer isNegotiable;

    @Min(value = 1, message = "商品状态非法")
    @Max(value = 2, message = "商品状态非法")
    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getOriginPrice() {
        return originPrice;
    }

    public void setOriginPrice(BigDecimal originPrice) {
        this.originPrice = originPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getSubCategoryId() {
        return subCategoryId;
    }

    public void setSubCategoryId(Integer subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public String getConditionLevel() {
        return conditionLevel;
    }

    public void setConditionLevel(String conditionLevel) {
        this.conditionLevel = conditionLevel;
    }

    public Integer getIsNegotiable() {
        return isNegotiable;
    }

    public void setIsNegotiable(Integer isNegotiable) {
        this.isNegotiable = isNegotiable;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
