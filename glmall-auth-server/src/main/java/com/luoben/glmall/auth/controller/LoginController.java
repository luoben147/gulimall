package com.luoben.glmall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.luoben.common.constant.AuthServerConstant;
import com.luoben.common.exception.BizCodeEnume;
import com.luoben.common.utils.R;
import com.luoben.common.vo.MemberResponseVO;
import com.luoben.glmall.auth.feign.MemberFeignService;
import com.luoben.glmall.auth.feign.ThirdPartyFeignService;
import com.luoben.glmall.auth.vo.UserLoginVo;
import com.luoben.glmall.auth.vo.UserRegistVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Resource
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    MemberFeignService memberFeignService;


    @GetMapping("/sms/sendcode")
    @ResponseBody
    public R sendCode(String phone) {

        //TODO 接口防刷

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            long saveTime = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - saveTime < 60 * 1000) {
                //60秒内 不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        //验证码再次校验   redis保存 key->phone value->code  sms:code:13812345678 - 4561
        String code = UUID.randomUUID().toString().substring(0, 5);
        String redisStorage =code+"_"+System.currentTimeMillis();
        //redis 缓存验证码 10分钟  防止同一phone 在60秒内再次发送
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,redisStorage,10,TimeUnit.MINUTES);

        thirdPartyFeignService.sendSMSCode(phone,code);
        return R.ok();
    }


    /**
     * //TODO 重定向携带数据,利用session原理，将数据放在session中，
     *      只要跳到下一个页面取出数据后，session里的数据就会删掉
     * // TODO  分布式下的session问题
     * RedirectAttributes   模拟重定向携带数据
     * @param registVo
     * @param result
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo registVo, BindingResult result, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(fieldError -> {
                return fieldError.getField();
            }, fieldError -> {
                return fieldError.getDefaultMessage();
            }));

            redirectAttributes.addFlashAttribute("errors",errors);
            //校验出错
            return "redirect:http://auth.glmall.com/reg.html";
        }


        //1.校验验证码
        String code = registVo.getCode();
        String phone = registVo.getPhone();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(s)){
            if(code.equals(s.split("_")[0])){
                //删除验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
                //验证码通过  调用远程服务注册
                R r = memberFeignService.regist(registVo);
                if(r.getCode()==0){
                    //成功
                    return "redirect:http://auth.glmall.com/login.html";
                }else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    return "redirect:http://auth.glmall.com/reg.html";
                }
            }else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                return "redirect:http://auth.glmall.com/reg.html";
            }
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.glmall.com/reg.html";
        }
    }


    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){

        //远程登录
        R r = memberFeignService.login(vo);
        if(r.getCode()==0){
            MemberResponseVO loginUser = r.getData(new TypeReference<MemberResponseVO>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, loginUser);
            return "redirect:http://glmall.com";
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:redirect:http://auth.glmall.com/login.html";
        }
    }


    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute==null){
            //没登陆
            return "login";
        }else {
            return "redirect:http://glmall.com";
        }
    }

}
