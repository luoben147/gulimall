package com.luoben.glmall.order.to;

import com.luoben.glmall.order.entity.OrderEntity;
import com.luoben.glmall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {

    private OrderEntity order;
    private List<OrderItemEntity> items;

    //订单计算的应付价格
    private BigDecimal payPrice;
    //运费
    private BigDecimal fare;

}
