package com.luoben.common.to.mq;

import lombok.Data;


/**
 * 库存锁定 消息队列传递对象
 */
@Data
public class StockLockedTo {

    private Long id;//库存工作单id

    private StockDetailTo detailTo;//工作单详情
}
