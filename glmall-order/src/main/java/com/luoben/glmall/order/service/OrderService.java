package com.luoben.glmall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.order.entity.OrderEntity;
import com.luoben.glmall.order.vo.*;

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

    /**
     * 根据订单号查询订单信息
     * @param orderSn
     * @return
     */
    OrderEntity getOrderByOrderSn(String orderSn);

    /**
     * 关闭订单
     * @param entity
     */
    void closeOrder(OrderEntity entity);

    /**
     * 获取当前订单的支付信息
     * @param orderSn
     * @return
     */
    PayVo getOrderPay(String orderSn);

    /**
     * 分页查询当前登录用户的所有订单信息
     * @param params
     * @return
     */
    PageUtils queryPageWithItem(Map<String,Object> params);

    /**
     * 处理支付宝的异步通知结果
     * @param vo
     * @return
     */
    String handPayResult(PayAsyncVo vo);

    /**
     * 支付宝手动关闭支付订单
     */
    void alipayTradeClose(OrderEntity entity);

    /**
     * 根据订单号查询支付宝支付状态
     * @param orderSn
     * @return
     */
    String queryPayStatus(String orderSn);
}

