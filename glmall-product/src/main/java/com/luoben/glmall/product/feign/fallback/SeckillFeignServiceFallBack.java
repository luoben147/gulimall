package com.luoben.glmall.product.feign.fallback;

import com.luoben.common.exception.BizCodeEnume;
import com.luoben.common.utils.R;
import com.luoben.glmall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 熔断
 */
@Slf4j
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.info("getSkuSeckillInfo熔断方法调用...");
        return R.error(BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getCode(),BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getMsg());
    }
}
