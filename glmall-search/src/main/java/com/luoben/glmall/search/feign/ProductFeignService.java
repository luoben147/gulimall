package com.luoben.glmall.search.feign;

import com.luoben.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("glmall-product")
public interface ProductFeignService {

    @GetMapping("/product/attr/info/{attrId}")
    public R attrsInfo(@PathVariable("attrId") Long attrId);

    @GetMapping("/product/brand/infos")
    public R getBrands(@RequestParam("brandIds") List<Long> brandIds);
}
