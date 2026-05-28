package com.segroup8.platform.vo;

public class ShopPublicVO {

    private Long id;
    private Long ownerUserId;
    private String name;
    private String description;
    private String logo;
    private String bannerUrl;
    private String region;
    private String category;
    private String businessHours;
    private String returnPolicy;
    private String shippingPolicy;
    private String announcement;
    private String decorationJson;
    private SellerRatingVO rating;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(String businessHours) {
        this.businessHours = businessHours;
    }

    public String getReturnPolicy() {
        return returnPolicy;
    }

    public void setReturnPolicy(String returnPolicy) {
        this.returnPolicy = returnPolicy;
    }

    public String getShippingPolicy() {
        return shippingPolicy;
    }

    public void setShippingPolicy(String shippingPolicy) {
        this.shippingPolicy = shippingPolicy;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    public String getDecorationJson() {
        return decorationJson;
    }

    public void setDecorationJson(String decorationJson) {
        this.decorationJson = decorationJson;
    }

    public SellerRatingVO getRating() {
        return rating;
    }

    public void setRating(SellerRatingVO rating) {
        this.rating = rating;
    }
}
