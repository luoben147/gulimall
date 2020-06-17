package com.luoben.glmall.product.vo;

import lombok.Data;

/**
 * 某个属性值关联的商品的id
 * 例如：  颜色：黑色 的 关联的sku
 */
@Data
public class AttrValueWithSkuIdVo {

    private String attrValue;
    private String skuIds; //多个以,分隔
}
