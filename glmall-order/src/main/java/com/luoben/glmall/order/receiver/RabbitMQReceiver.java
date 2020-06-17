package com.luoben.glmall.order.receiver;

import com.luoben.glmall.order.entity.OrderEntity;
import com.luoben.glmall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RabbitListener(queues = {"hello-java-queue"})
@Component
public class RabbitMQReceiver {

    /**
     * queues: 声明需要监听的所有队列
     *
     * 接收到的消息类型： org.springframework.amqp.core.Message
     *
     * 参数Message可以写以下类型；
     *  1.Message message ：原生消息详细信息，头+体
     *  2.T<发送的消息的类型> OrderReturnReasonEntity content
     *  3.Channel channel:  当前传输数据的通道
     *
     *  Queue：  可以有很多人监听。只要收到消息，队列删除消息，而且只能有一个收到此消息
     *  场景：
     *      1）、订单服务启动多个,同一个消息只能有一个客户端收到
     *      2）、只有一个消息完全处理完，方法运行结束，才接收到下一个消息
     *
     */
    //@RabbitListener(queues = {"hello-java-queue"})
    @RabbitHandler
    public void receiveMessage(Message message,
                               OrderReturnReasonEntity content,
                               Channel channel) throws  IOException {
        //{"id":1,"name":"哈哈","sort":null,"status":null,"createTime":1592294151728}
        System.out.println("接受到消息.."+content);
        byte[] body = message.getBody();
        //消息头 属性信息
        MessageProperties properties = message.getMessageProperties();
        //Thread.sleep(3000);
        System.out.println("消息处理完成==>"+content.getName());

        //channel内按顺序自增的
        long deliveryTag = properties.getDeliveryTag();
        //确认消息已消费   非批量模式
        try {
            channel.basicAck(deliveryTag,false);
        } catch (IOException e) {
            e.printStackTrace();
            //退签  requeue:是否重新入队  requeue=false 丢弃  requeue=true 发回服务器，服务器重新入队
            // basicNack(long deliveryTag, boolean multiple, boolean requeue)
            channel.basicNack(deliveryTag,false,true);
            //退签
            // basicReject(long deliveryTag, boolean requeue)
            //channel.basicReject();
        }

    }


    @RabbitHandler
    public void receiveMessage2(OrderEntity content,
                                Channel channel){

        System.out.println("接受到消息.."+content);
    }
}
