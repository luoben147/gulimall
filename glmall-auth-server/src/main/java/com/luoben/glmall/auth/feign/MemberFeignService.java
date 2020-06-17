package com.luoben.glmall.auth.feign;

import com.luoben.common.utils.R;
import com.luoben.glmall.auth.vo.SocialUser;
import com.luoben.glmall.auth.vo.UserLoginVo;
import com.luoben.glmall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("glmall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/regist")
    public R regist(@RequestBody UserRegistVo vo);


    @PostMapping("/member/member/login")
    public R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser) throws Exception;
}
