package com.luoben.glmall.member.exception;

public class PhoneExistExcetpion extends RuntimeException{

    public PhoneExistExcetpion() {
        super("手机号已存在");
    }
}
