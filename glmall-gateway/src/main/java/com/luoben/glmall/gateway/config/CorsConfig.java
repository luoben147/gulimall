package com.luoben.glmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        //添加CORS配置信息
        CorsConfiguration configuration=new CorsConfiguration();
        //配置跨域
        //允许的头信息  *代表允许携带任何头信息
        configuration.addAllowedHeader("*");
        // 允许的请求方式  *代表所有的请求方法 GET POST PUT DELETE...
        configuration.addAllowedMethod("*");
        //允许的请求来源 * 代表所有域名都可以跨域访问
        configuration.addAllowedOrigin("*");
        //是否允许携带Cookie信息
        configuration.setAllowCredentials(true);


        //添加映射路径，/** 拦截一切请求
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",configuration);

        return new CorsWebFilter(source);
    }
}
