package com.luoben.glmall.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 */
@Configuration
@EnableAsync    //开启异步任务
@EnableScheduling
public class ScheduleConfig {

}
