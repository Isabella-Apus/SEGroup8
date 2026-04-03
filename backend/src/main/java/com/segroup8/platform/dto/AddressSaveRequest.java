package com.segroup8.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AddressSaveRequest {

    @NotBlank(message = "收件人不能为空")
    @Size(max = 50, message = "收件人长度不能超过50")
    private String receiverName;

    @NotBlank(message = "收件人电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "收件人电话需为11位手机号")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    @Size(max = 50, message = "省份长度不能超过50")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市长度不能超过50")
    private String city;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255")
    private String detailAddress;

    @NotNull(message = "是否默认地址不能为空")
    private Integer isDefault;

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }
}
