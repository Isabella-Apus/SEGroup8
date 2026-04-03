package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MerchantApplicationSubmitRequest {

    @NotBlank(message = "店名不能为空")
    @Size(max = 80, message = "店名长度不能超过80")
    private String storeName;

    @NotNull(message = "主营领域不能为空")
    private Integer categoryId;

    @NotBlank(message = "身份证号不能为空")
    @Size(max = 30, message = "身份证号长度不能超过30")
    private String idCardNo;

    @NotBlank(message = "银行卡号不能为空")
    @Size(max = 50, message = "银行卡号长度不能超过50")
    private String bankCardNo;

    @NotBlank(message = "营业执照不能为空")
    @Size(max = 255, message = "营业执照地址长度不能超过255")
    private String licenseImg;

    @NotBlank(message = "仓库地址不能为空")
    @Size(max = 255, message = "仓库地址长度不能超过255")
    private String warehouseAddr;

    @NotBlank(message = "仓库省份不能为空")
    @Size(max = 50, message = "仓库省份长度不能超过50")
    private String warehouseProvince;

    @NotBlank(message = "仓库城市不能为空")
    @Size(max = 50, message = "仓库城市长度不能超过50")
    private String warehouseCity;

    @NotBlank(message = "仓库详细地址不能为空")
    @Size(max = 255, message = "仓库详细地址长度不能超过255")
    private String warehouseDetail;

    @NotBlank(message = "业务负责人姓名不能为空")
    @Size(max = 50, message = "负责人姓名长度不能超过50")
    private String contactName;

    @NotBlank(message = "业务负责人电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "负责人电话需为11位手机号")
    private String contactPhone;

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getBankCardNo() {
        return bankCardNo;
    }

    public void setBankCardNo(String bankCardNo) {
        this.bankCardNo = bankCardNo;
    }

    public String getLicenseImg() {
        return licenseImg;
    }

    public void setLicenseImg(String licenseImg) {
        this.licenseImg = licenseImg;
    }

    public String getWarehouseAddr() {
        return warehouseAddr;
    }

    public void setWarehouseAddr(String warehouseAddr) {
        this.warehouseAddr = warehouseAddr;
    }

    public String getWarehouseProvince() {
        return warehouseProvince;
    }

    public void setWarehouseProvince(String warehouseProvince) {
        this.warehouseProvince = warehouseProvince;
    }

    public String getWarehouseCity() {
        return warehouseCity;
    }

    public void setWarehouseCity(String warehouseCity) {
        this.warehouseCity = warehouseCity;
    }

    public String getWarehouseDetail() {
        return warehouseDetail;
    }

    public void setWarehouseDetail(String warehouseDetail) {
        this.warehouseDetail = warehouseDetail;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
