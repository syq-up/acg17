package com.shiyq.entity.VO;

import lombok.Data;

@Data
public class UserInfoVO {
    private String username;
    private String nickname;
    private String avatarUrl;
    private Long illustrationCount;
    private Long illustrationStorageBytes;
    private Long mangaCount;
    private Long mangaStorageBytes;
    private Long gameCount;
    private Long novelCount;
    private Long novelWords;
    private String createTime;
}
