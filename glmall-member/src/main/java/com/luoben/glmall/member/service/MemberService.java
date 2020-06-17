package com.luoben.glmall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.member.entity.MemberEntity;
import com.luoben.glmall.member.exception.PhoneExistExcetpion;
import com.luoben.glmall.member.exception.UserNameExistExcetpion;
import com.luoben.glmall.member.vo.MemberLoginVo;
import com.luoben.glmall.member.vo.MemberRegistVo;
import com.luoben.glmall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:33:53
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistExcetpion;

    void checkUserNameUnique(String userName) throws UserNameExistExcetpion;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;
}

