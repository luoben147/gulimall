package com.luoben.glmall.member.exception;

public class UserNameExistExcetpion extends RuntimeException{
    public UserNameExistExcetpion() {
        super("用户名已存在");
    }
}
