package com.segroup8.platform.vo;

public class SecondhandSellerPublicVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private String region;
    private SellerRatingVO rating;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public SellerRatingVO getRating() {
        return rating;
    }

    public void setRating(SellerRatingVO rating) {
        this.rating = rating;
    }
}
