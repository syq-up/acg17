package com.shiyq.entity.VO;

public class LoginVO {
    private final String accessToken;

    public LoginVO(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
