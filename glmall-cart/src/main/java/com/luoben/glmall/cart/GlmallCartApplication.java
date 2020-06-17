package com.luoben.glmall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(exclude =DataSourceAutoConfiguration.class)
public class GlmallCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlmallCartApplication.class, args);
    }

}
