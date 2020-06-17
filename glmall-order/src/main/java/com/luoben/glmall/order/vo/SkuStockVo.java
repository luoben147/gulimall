package com.luoben.glmall.order.vo;

import lombok.Data;

/**
 * 库存状态
 */
@Data
public class SkuStockVo {
    private Long skuId;
    private Boolean hasStock;
}
