package com.luoben.glmall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.luoben.common.constant.AuthServerConstant;
import com.luoben.common.utils.HttpUtils;
import com.luoben.common.utils.R;
import com.luoben.glmall.auth.feign.MemberFeignService;
import com.luoben.common.vo.MemberResponseVO;
import com.luoben.glmall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 社交登陆
 */
@Controller
@Slf4j
public class OAuth2Controller {

    @Resource
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        Map<String,String> map=new HashMap<>();
        map.put("client_id","2360232986");
        map.put("client_secret","979a4c02ad9d19db32d83852839ce146");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://auth.glmall.com/oauth2.0weibo/success");
        map.put("code",code);
        //根据code 换取accessToken
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", new HashMap<>(), map, new HashMap<>());


        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            // 获取用户的登录平台，然后判断用户是否该注册到系统中
            R r = memberFeignService.oauthLogin(socialUser);
            if (r.getCode() == 0) {

                MemberResponseVO loginUser = r.getData(new TypeReference<MemberResponseVO>() {});
                log.info("登陆成功,用户：{}",loginUser);
                // session 子域共享问题
                session.setAttribute(AuthServerConstant.LOGIN_USER, loginUser);

                //成功 跳转首页
                return "redirect:http://glmall.com";
            } else {
                return "redirect:http://auth.glmall.com/login.html ";
            }
        }else {
            return "redirect:http://auth.glmall.com/login.html ";
        }
    }

}
