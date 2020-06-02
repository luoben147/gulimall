package com.luoben.glmall.ware.feign;

import com.luoben.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("glmall-product")
public interface ProductFeignService {

    /**
     * /product/skuinfo/info/{skuId}
     *
     * 1)、让所有请求过网关；
     *    1、@FeignClient("glmall-gateway")：给glmall-gateway所在的机器发请求
     *    2、/api/product/skuinfo/info/{skuId}
     * 2）、直接让后台指定服务处理
     *    1、@FeignClient("glmall-product")
     *    2、/product/skuinfo/info/{skuId}
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
