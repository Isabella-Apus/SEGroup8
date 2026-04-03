package com.segroup8.platform.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class UserProfileUpdateRequest {

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @Size(max = 255, message = "头像地址长度不能超过255")
    private String avatar;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号需为11位")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

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
}
