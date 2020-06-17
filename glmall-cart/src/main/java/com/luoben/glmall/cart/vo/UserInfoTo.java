package com.luoben.glmall.cart.vo;

import lombok.Data;

@Data
public class UserInfoTo {

    private Long userId;
    private String userKey;
    //是否临时用户
    private Boolean tempUser=false;
}
