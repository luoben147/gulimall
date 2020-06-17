package com.luoben.glmall.product;

import com.luoben.glmall.product.entity.BrandEntity;
import com.luoben.glmall.product.service.AttrGroupService;
import com.luoben.glmall.product.service.BrandService;
import com.luoben.glmall.product.service.CategoryService;
import com.luoben.glmall.product.service.SkuSaleAttrValueService;
import com.luoben.glmall.product.vo.SkuItemSaleAttrVo;
import com.luoben.glmall.product.vo.SpuItemAttrGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GlmallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService  skuSaleAttrValueService;

    @Test
    public void testGetSaleAttrsBySpuId() {
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueService.getSaleAttrsBySpuId(1L);
        System.out.println(saleAttrsBySpuId);
    }

    @Test
    public void testGetAttrGroupWithAttrsBySpuId() {
        List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupService.getAttrGroupWithAttrsBySpuId(1L, 225L);
        System.out.println(attrGroupWithAttrsBySpuId);
    }

    @Test
    public void testRedisson() {

    }

    @Test
    public void testStringRedisTemplate() {

        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        //保存
        ops.set("hello","world_"+UUID.randomUUID());

        //查询
        String hello = ops.get("hello");
        System.out.println("redis保存的数据："+hello);
    }

    @Test
    public void testFindCateLogPath() {
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("分类完整ids:{}", Arrays.asList(catelogPath));
    }

    @Test
    public void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//        System.out.println("保存成功    ");

//        brandEntity.setBrandId(1L);
//        brandEntity.setDescript("啊啊啊啊");
//        brandService.updateById(brandEntity);
    }

}
