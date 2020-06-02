package com.luoben.glmall.product;

import com.luoben.glmall.product.entity.BrandEntity;
import com.luoben.glmall.product.service.BrandService;
import com.luoben.glmall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GlmallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

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
