package com.luoben.glmall.ware.listener;

import com.luoben.common.to.mq.OrderTo;
import com.luoben.common.to.mq.StockLockedTo;
import com.luoben.glmall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 监听解锁库存消息队列
 */
@RabbitListener(queues = "stock.release.stock.queue")
@Component
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     * 监听解锁库存消息
     * 只要解锁库存的消息失败，一定要告诉服务解锁失败
     * @param to
     * @param message
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息");
        try {
            //当前消息是否被第二次及以后（重新）派发过来的
//            Boolean redelivered = message.getMessageProperties().getRedelivered();
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            //消息拒绝以后重新放到队列中，让别人继续消费解锁
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    //监听 订单关闭消息  订单关闭，执行解锁库存
    @RabbitHandler
    public void handleOrderCloseRelease(Message message,Channel channel,OrderTo orderTo) throws IOException {
        System.out.println("订单关闭，准备解锁库存....");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

}
