package com.luoben.glmall.order.feign;

import com.luoben.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("glmall-product")
public interface ProductFeignService {
    /**
     * 根据skuId查询Spu信息
     * @param skuId
     * @return
     */
    @GetMapping("/product/spuinfo/skuId/{id}")
    public R getSpuInfoBySkuId(@PathVariable("id") Long skuId);
}
