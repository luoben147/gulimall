package com.luoben.glmall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

//销售属性组合
@ToString
@Data
public class SkuItemSaleAttrVo {

    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
