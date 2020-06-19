package com.luoben.glmall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.to.mq.OrderTo;
import com.luoben.common.to.mq.StockLockedTo;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.ware.entity.WareSkuEntity;
import com.luoben.glmall.ware.vo.SkuHasStockVo;
import com.luoben.glmall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:54:50
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    /**
     * 查询sku是否有库存
     * @param skuIds
     * @return
     */
    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    /**
     * 锁定订单商品库存
     * @param lockVo
     * @return
     */
    Boolean orderLockStock(WareSkuLockVo lockVo);

    /**
     * 解锁库存
     * @param to
     */
    void unlockStock(StockLockedTo to);

    /**
     * 解锁库存
     * @param orderTo
     */
    void unlockStock(OrderTo orderTo);
}

