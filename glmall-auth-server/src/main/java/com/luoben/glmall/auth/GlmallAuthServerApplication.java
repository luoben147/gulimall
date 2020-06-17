package com.luoben.glmall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


/**
 * Spring Session 核心原理
 *  1。@EnableRedisHttpSession 导入配置RedisHttpSessionConfiguration
 *      1.给容器中添加了一个组件
 *         SessionRepository =>> 【RedisIndexedSessionRepository】 =》》 redsi操作session， session的增删改查封装类
 *      2.SessionRepositoryFilter ==》Filter ： session存储过滤器 ;每个请求过来都必须经过filter
 *          1.创建的时候，自动从容器中获取到了SessionRepository
 *          2.原始的request和response都被包装。SessionRepositoryRequestWrapper，SessionRepositoryResponseWrapper
 *          3.以后获取session。 request.getSession();
 *          4.wrappedRequest.getSession();  ==>> SessionRepository中获取到的
 *
 *   装饰着模式
 */

@EnableRedisHttpSession     //整合redis作为session存储
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class GlmallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlmallAuthServerApplication.class, args);
    }

}
