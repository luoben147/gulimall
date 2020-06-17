package com.luoben.glmall.cart.interceptor;

import com.luoben.common.constant.AuthServerConstant;
import com.luoben.common.constant.CartConstant;
import com.luoben.common.vo.MemberResponseVO;
import com.luoben.glmall.cart.vo.UserInfoTo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在执行目标方法之前，判断用户登录状态。并封装传递给目标请求
 */
public class CartInterceptor implements HandlerInterceptor {


    public static ThreadLocal<UserInfoTo> threadLocal=new InheritableThreadLocal<>();


    /**
     * 目标方法执行之前
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        UserInfoTo userInfoTo = new UserInfoTo();

        HttpSession session = request.getSession();
        MemberResponseVO member = (MemberResponseVO)session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(member!=null){
            //用户登录了
            userInfoTo.setUserId(member.getId());
        }

        Cookie[] cookies = request.getCookies();
        if(cookies!=null&&cookies.length>0){
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if(name.equals(CartConstant.TEMP_USER_COOKIE_NAME)){
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }

        //如果没有临时用户，分配一个临时用户id
        if(StringUtils.isEmpty(userInfoTo.getUserKey())){
            String uuid=UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }
        threadLocal.set(userInfoTo);
        return true; //放行
    }


    /**
     * 业务执行之后
     * 分类临时用户，让浏览器保存
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        UserInfoTo userInfoTo = threadLocal.get();

        //没有临时用户，保存一个临时用户
        if(!userInfoTo.getTempUser()){
            //持续延长临时用户的过期时间
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("glmall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
