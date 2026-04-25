package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String role;
    private String status;
    /**
     * 买家信用分（所有用户都有）
     * 对应 DB 列：credit_score
     */
    private Integer creditScore;
    private String shopName;
    private String shopDesc;
    private String bannerUrl;
    private String category;
    private String region;
    private String businessHours;
    private String returnPolicy;
    private String shippingPolicy;
    private String announcement;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // TODO: 关联商品/订单/评价数统计 的逻辑接口

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    /**
     * 店铺账户健康分（仅 OFFICIAL_SELLER 有意义）
     * 对应 DB 列：seller_credit_score
     */
    @TableField("seller_credit_score")
    private Integer sellerCreditScore;

    public Integer getSellerCreditScore() { return sellerCreditScore; }
    public void setSellerCreditScore(Integer sellerCreditScore) { this.sellerCreditScore = sellerCreditScore; }

    /**
     * 二手卖家信用分（所有用户都有，用于二手交易卖家侧评价）
     * 对应 DB 列：buyer_credit_score
     *
     * 注意：该列由 schema 的 ALTER TABLE 添加，复用 buyer_credit_score 存储二手卖家分。
     * 买家信用统一使用 credit_score（creditScore 字段）。
     */
    @TableField("buyer_credit_score")
    private Integer shSellerCreditScore;

    public Integer getShSellerCreditScore() { return shSellerCreditScore; }
    public void setShSellerCreditScore(Integer shSellerCreditScore) { this.shSellerCreditScore = shSellerCreditScore; }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopDesc() { return shopDesc; }
    public void setShopDesc(String shopDesc) { this.shopDesc = shopDesc; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }

    public String getReturnPolicy() { return returnPolicy; }
    public void setReturnPolicy(String returnPolicy) { this.returnPolicy = returnPolicy; }

    public String getShippingPolicy() { return shippingPolicy; }
    public void setShippingPolicy(String shippingPolicy) { this.shippingPolicy = shippingPolicy; }

    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; }
}