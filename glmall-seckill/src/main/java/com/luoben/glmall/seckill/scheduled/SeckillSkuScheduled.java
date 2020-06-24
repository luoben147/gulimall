package com.luoben.glmall.seckill.scheduled;


import com.luoben.glmall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品定时上架
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    private final String upload_lock = "seckill:upload:lock";

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedissonClient redissonClient;

    /**
     * 每天晚上3点上架秒杀最近3天上架的商品
     *  TODO 幂等性处理
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void uploadSeckillSkuLates30Days() {
        //1、重复上架无需处理
        log.warn("上架商品秒杀信息.........");
        //分布式锁，锁的业务执行完成后状态就已经更新，释放锁后，其他线程获取到该锁之后就会得到最新的状态
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10,TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLates30Days();
        } finally {
            lock.unlock();
        }
    }
}
