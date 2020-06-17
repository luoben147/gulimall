package com.luoben.glmall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.order.entity.OrderEntity;
import com.luoben.glmall.order.vo.OrderConfirmVo;
import com.luoben.glmall.order.vo.OrderSubmitVo;
import com.luoben.glmall.order.vo.SubmitOrderResponseVo;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:47:07
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询订单确认页需要的数据
     * @return
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    /**
     * 下单
     * @param vo
     * @return
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);
}

