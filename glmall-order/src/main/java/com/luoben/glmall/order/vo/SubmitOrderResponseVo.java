package com.luoben.glmall.order.vo;

import com.luoben.glmall.order.entity.OrderEntity;
import lombok.Data;

/**
 * 下单返回数据vo
 */
@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;
    //状态码 0==》成功
    private Integer code;

}
