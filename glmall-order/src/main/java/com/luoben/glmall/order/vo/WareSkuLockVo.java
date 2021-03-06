package com.luoben.glmall.order.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁定商品库存vo
 */
@Data
public class WareSkuLockVo {

    //订单号
    private String orderSn;

    //需要锁定库存的所有订单项
    private List<OrderItemVo> locks;
}
