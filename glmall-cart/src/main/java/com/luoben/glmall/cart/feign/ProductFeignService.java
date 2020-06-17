package com.luoben.glmall.cart.feign;

import com.luoben.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("glmall-product")
public interface ProductFeignService {

    /**
     * 获取sku信息
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R getSkuInfo(@PathVariable("skuId") Long skuId);


    /**
     * 根据skuId 获取商品的属性组合
     * @param skuId
     * @return
     */
    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    public List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);

    /**
     * 获取商品最新的价格
     * @param skuId
     * @return
     */
    @GetMapping("/product/skuinfo/{skuId}/price")
    public R getSkuPrice(@PathVariable("skuId") Long skuId);

}
