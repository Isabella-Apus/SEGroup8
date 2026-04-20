package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @Size(max = 255, message = "头像地址长度不能超过255")
    private String avatar;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号需为11位")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @Size(max = 100, message = "店铺名称不能超过100")
    private String shopName;

    @Size(max = 200, message = "店铺简介不能超过200")
    private String shopDesc;

    @Size(max = 255, message = "封面地址不能超过255")
    private String bannerUrl;

    @Size(max = 50, message = "类目不能超过50")
    private String category;

    @Size(max = 100, message = "地区不能超过100")
    private String region;

    @Size(max = 100, message = "营业时间不能超过100")
    private String businessHours;

    @Size(max = 500, message = "退换货政策不能超过500")
    private String returnPolicy;

    @Size(max = 300, message = "发货说明不能超过300")
    private String shippingPolicy;

    @Size(max = 300, message = "店铺公告不能超过300")
    private String announcement;

    @Size(max = 50, message = "店铺负责人姓名不能超过50")
    private String shopContactName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "店铺负责人手机号需为11位")
    private String shopContactPhone;

    @Size(max = 255, message = "仓库地址不能超过255")
    private String warehouseAddr;
}