package com.luoben.glmall.order;

import com.luoben.glmall.order.entity.OrderEntity;
import com.luoben.glmall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GlmallOrderApplicationTests {

    /**
     * Exchange类型：
     *   DirectExchange： 点对点精确传递
     *   FanoutExchange: 广播传递类型，不管路由键（Routing key）是否匹配，只要绑定了的Queue都会收到消息
     *   TopicExchange:  模糊匹配类型，Routing key 支持通配 *.news  aa.#   路由键匹配的会收到消息
     *
     * 1.创建Exchange[hello.java.exchange] 、Queue、Binding
     *      1） AmqpAdmin 创建
     * 2.收发消息
     */

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    void sendMessageTest(){

        //1.发送消息, 如果发送的消息是对象，会使用序列化机制，将对象写出去。对象必须实现 Serializable
        String msg="hello word!";

        //2.发送的对象类型的消息，可以是json  配置MessageConverter 为Jackson2JsonMessageConverter

        for (int i=0;i<10;i++){
            if(i%2==0){
                OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                reasonEntity.setId(1L);
                reasonEntity.setCreateTime(new Date());
                reasonEntity.setName("哈哈-" + i);
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reasonEntity);
            }else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderEntity);
            }
            log.info("消息发送完成:{}");
        }

    }

    @Test
    void createExchange() {

        /**
         * 交换机名称，  是否持久化， 是否自动删除， 参数
         * DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
         */
        DirectExchange directExchange = new DirectExchange("hello-java-exchange",true,false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功","hello-java-exchange");
    }

    @Test
    void createQueue() {
        /**
         * 队列名称，是否持久化，是否排他（声明它的才能连接它），是否自动删除，参数
         * Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
         */
        Queue queue = new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功","hello-java-queue");
    }


    @Test
    void createBinding() {

        /**
         * Binding(String destination【目的地】,
         * Binding.DestinationType destinationType【目的地类型】,
         * String exchange 【交换机】,
         * String routingKey【路由键】,
         * Map<String, Object> arguments【参数】)
         *
         * 将exchange制定的交换机和destination目的地进行绑定，使用routingKey作为指定路由键
         */
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",
                null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功","hello-java-binding");
    }

}
