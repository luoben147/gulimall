package com.luoben.glmall.cart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session 配置
 */
@EnableRedisHttpSession
@Configuration
public class GlmallSessionConfig {

    /**
     * 自定义cookie序列化
     * @return
     */
    @Bean
    public CookieSerializer cookieSerializer(){
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();

        cookieSerializer.setDomainName("glmall.com");//指定作用域  扩大作用域到父域
        cookieSerializer.setCookieName("GLSESSION");//指定名字
        return cookieSerializer;
    }

    /**
     * session存储到redis序列化 使用json
     * @return
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
            return new GenericJackson2JsonRedisSerializer();
    }

}
