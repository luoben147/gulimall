package com.luoben.glmall.ware.feign;

import com.luoben.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("glmall-order")
public interface OrderFeignService {
    /**
     * 根据订单号查询订单
     * @param orderSn
     * @return
     */
    @GetMapping("/order/order/status/{orderSn}")
    public R getOrderByOrderSn(@PathVariable("orderSn") String orderSn);
}
