package com.luoben.glmall.order.interceptor;

import com.luoben.common.constant.AuthServerConstant;
import com.luoben.common.vo.MemberResponseVO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVO> loginUser=new InheritableThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        MemberResponseVO attribute = (MemberResponseVO)request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute!=null){
            loginUser.set(attribute);
            return true;
        }else {
            request.getSession().setAttribute("msg","请先登录！");
            response.sendRedirect("http://auth.glmall.com/login.html");

            //没登陆 去登录
            return false;
        }

    }
}
