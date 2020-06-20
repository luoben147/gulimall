package com.luoben.glmall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;


@Configuration
public class GlFeignConfig {

    /**
     * 配置feign远程调用的请求拦截器
     * 解决feign远程调用请求头丢失问题
     * @return
     */
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){

        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {

                ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
                if(attributes!=null){
                    HttpServletRequest request = attributes.getRequest();
                    if(request!=null){
                        //同步请求头数据，Cookie
                        String cookie = request.getHeader("Cookie");
                        requestTemplate.header("Cookie",cookie);
                    }
                }
            }
        };
    }

}
