package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;

public class OrderShipRequest {

    @Size(max = 50, message = "发货省份长度不能超过50")
    private String originProvince;

    @Size(max = 50, message = "发货城市长度不能超过50")
    private String originCity;

    @Size(max = 255, message = "发货详细地址长度不能超过255")
    private String originDetail;

    public String getOriginProvince() {
        return originProvince;
    }

    public void setOriginProvince(String originProvince) {
        this.originProvince = originProvince;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getOriginDetail() {
        return originDetail;
    }

    public void setOriginDetail(String originDetail) {
        this.originDetail = originDetail;
    }
}
