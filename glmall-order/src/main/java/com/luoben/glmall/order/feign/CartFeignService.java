package com.luoben.glmall.order.feign;

import com.luoben.glmall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("glmall-cart")
public interface CartFeignService {

    /**
     * 获取当前用户的所有选中的购物项
     * @return
     */
    @GetMapping("/currentUserCartItems")
    public List<OrderItemVo> getCurrentUserCartItems();
}
