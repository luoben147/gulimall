package com.luoben.glmall.seckill.feign;

import com.luoben.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("glmall-coupon")
public interface CouponFeignService {

    /**
     * 获取最近三天的秒杀活动
     * @return
     */
    @GetMapping("/coupon/seckillsession/lates3DaySession")
    public R getLates3DaySession();
}
