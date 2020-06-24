package com.luoben.glmall.product.vo;

import com.luoben.glmall.product.entity.SkuImagesEntity;
import com.luoben.glmall.product.entity.SkuInfoEntity;
import com.luoben.glmall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    //1.sku基本信息 pms_sku_info
    SkuInfoEntity info;

    //是否有货
    Boolean hasStock=true;

    //2.sku 图片  pms_sku_images
    List<SkuImagesEntity> images;

    //3.spu 销售属性（sku所有组合）
    List<SkuItemSaleAttrVo> saleAttr;

    //4.spu介绍
    SpuInfoDescEntity desp;

    //5.spu规格参数
    List<SpuItemAttrGroupVo> groupAttrs;

    //6.当前商品的秒杀优惠信息
    SeckillInfoVo seckillInfo;

}
