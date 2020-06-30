package com.luoben.glmall.seckill.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.luoben.common.to.mq.SeckillOrderTo;
import com.luoben.common.utils.R;
import com.luoben.common.vo.MemberResponseVO;
import com.luoben.glmall.seckill.feign.CouponFeignService;
import com.luoben.glmall.seckill.feign.ProductFeignService;
import com.luoben.glmall.seckill.interceptor.LoginUserInterceptor;
import com.luoben.glmall.seckill.service.SeckillService;
import com.luoben.glmall.seckill.to.SeckillSkuRedisTo;
import com.luoben.glmall.seckill.vo.SeckillSessionWithSkus;
import com.luoben.glmall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    CouponFeignService couponFeignService;

    @Resource
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;


    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus:";
    private final String SKU_STOCK_SEMAPHQRE = "seckill:stock:";


    @Override
    public void uploadSeckillSkuLates30Days() {
        //1.扫描最近三天需要参与秒杀的活动
        R r = couponFeignService.getLates3DaySession();
        if (r.getCode() == 0) {
            //上架
            List<SeckillSessionWithSkus> data = r.getData(new TypeReference<List<SeckillSessionWithSkus>>() {
            });
            //缓存到redis
            //1.缓存活动信息
            saveSessionInfos(data);
            //2.缓存活动的关联商品信息
            saveSessionSkuInfos(data);
        }
    }

    /**
     * 获取当前时间可以参与的秒杀商品信息
     *
     * blockHandler : 在原方法被限流/降级/系统保护的时候调用
     * fallback： 在抛出异常时提供的处理逻辑，针对所有类型的异常。
     *      返回值类型与原函数一样。
     *      方法参数与原函数一样。
     *      fallback方法需要与原方法在同一个类中。若在其他类中 则对应的函数必须为static
     * @return
     */
    @SentinelResource(blockHandler = "seckillBlockHandler")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1.确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();

        //redis查询 seckill:sessions:* 为key的数据
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            //seckill:sessions:1592848800000_1592852400000
            String replace = key.replace("seckill:sessions:", "");
            String[] s = replace.split("_");
            Long start = Long.parseLong(s[0]);
            Long end = Long.parseLong(s[1]);
            if (time >= start && time <= end) {
                //2.获取这个秒杀场次需要的所有商品信息
                //获取 场次_skuid
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redisTo = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
                        //redisTo.setRandomCode(null);  当前秒杀已经开始，就需要随机码
                        return redisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }


        return null;
    }


    //降级方法
    public List<SeckillSkuRedisTo> seckillBlockHandler(BlockException e){
        log.error("getCurrentSeckillSkus 被限流了");
        return null;
    }


    /**
     * 获取当前商品的秒杀信息
     *
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //1、找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            //正则匹配key   3_1
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);

                    //处理随机码
                    long current = new Date().getTime();
                    if (current >= skuRedisTo.getStartTime() && current <= skuRedisTo.getEndTime()) {
                    } else {
                        skuRedisTo.setRandomCode(null);
                    }
                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    /**
     * 秒杀
     * //TODO 上架秒杀商品的时候，每一个商品都有过期时间， 锁定库存，秒杀商品过期后解锁库存
     * //TODO 秒杀后续的流程，简化了收货地址等信息。
     *
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        //1.获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (StringUtils.isEmpty(json)) {
            return null;
        } else {
            SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
            //校验合法性
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long now = new Date().getTime();

            long ttl = endTime - startTime;

            if (now >= startTime && now <= endTime) {
                //2.校验随机码和商品id
                String randomCode = redisTo.getRandomCode();
                String rKey = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if (randomCode.equals(key) && killId.equals(rKey)) {
                    //3.验证购物数量是否合理
                    Integer seckillLimit = redisTo.getSeckillLimit();
                    if (num <= seckillLimit) {
                        //4.验证当前用户是否已经购买过 ；幂等性：如果秒杀成功，就去redis占位 userId_promotionSessionId_skuId
                        //SETNX
                        String isBuyKey = memberResponseVO.getId() + "_" + rKey;
                        //自动过期
                        Boolean isBuy = redisTemplate.opsForValue().setIfAbsent(isBuyKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (isBuy) {
                            //占位成功说明从来没买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHQRE + randomCode);

                            //在指定的时间内尝试获取x个许可,如果获取不到则返回false
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功
                                //快速下单，发送mq消息  10ms
                                String order_sn = IdWorker.getTimeId();
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(order_sn);
                                orderTo.setMemberId(memberResponseVO.getId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                orderTo.setSkuId(redisTo.getSkuId());
                                orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                return order_sn;
                            }
                            return null;

                        } else {
                            //占位失败 说明买过
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        return null;
    }

    /**
     * 缓存活动信息
     *
     * @param data
     */
    private void saveSessionInfos(List<SeckillSessionWithSkus> data) {
        if (data != null) {
            data.stream().forEach(s -> {
                Long startTime = s.getStartTime().getTime();
                Long endTime = s.getEndTime().getTime();
                String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
                //缓存活动信息
                Boolean hasKey = redisTemplate.hasKey(key);
                if (!hasKey) {
                    List<String> collect = s.getRelationSkus().stream().map(item ->
                            item.getPromotionSessionId() + "_" + item.getSkuId().toString()
                    ).collect(Collectors.toList());
                    redisTemplate.opsForList().leftPushAll(key, collect);
                }
            });
        }
    }


    /**
     * 缓存活动的关联商品信息
     *
     * @param data
     */
    private void saveSessionSkuInfos(List<SeckillSessionWithSkus> data) {
        if(data!=null) {
            data.stream().forEach(s -> {
                //准备hash操作
                BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

                s.getRelationSkus().stream().forEach(seckillSkuVo -> {
                    String token = UUID.randomUUID().toString().replace("-", "");

                    if (!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString())) {
                        //缓存秒杀商品
                        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                        //1、sku的基本信息
                        R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                        if (r.getCode() == 0) {
                            SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            redisTo.setSkuInfo(skuInfo);
                        }

                        //2、sku的秒杀信息
                        BeanUtils.copyProperties(seckillSkuVo, redisTo);

                        //3、设置当前商品的秒杀时间信息
                        redisTo.setStartTime(s.getStartTime().getTime());
                        redisTo.setEndTime(s.getEndTime().getTime());

                        //4、随机码
                        redisTo.setRandomCode(token);

                        String values = JSON.toJSONString(redisTo);
                        ops.put(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString(), values);

                        //5、使用库存作为分布式的信号量  限流
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHQRE + token);
                        //商品可以秒杀的数量作为信号量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                    }
                });
            });
        }
    }

}
