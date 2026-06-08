package com.segroup8.platform.dto;

public class OrderShipRequest {

    private String originProvince;

    private String originCity;

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
