package com.luoben.glmall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyRabbitConfig {

    /**
     * 配置消息类型转换器
     * 使用json序列化，进行消息转换
     * @return
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }


//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(Message message){
//
//    }

    @Bean
    public Exchange stockEventExchange(){
        TopicExchange topicExchange = new TopicExchange("stock-event-exchange", true, false);
        return topicExchange;
    }

    //解锁库存消息队列
    @Bean
    public Queue stockReleaseStockQueue(){
        Queue queue = new Queue("stock.release.stock.queue", true, false, false);
        return queue;
    }

    //库存死信队列  2分钟后发送路由键stock.release 到交换机stock-event-exchange
    //解锁库存消息队列Binding了路由键stock.release 所有 消息到了stock.release.stock.queue解锁库存消息队列
    //监听stock.release.stock.queue 触发消息过期的逻辑 解锁库存
    @Bean
    public Queue stockDelayQueue(){
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("x-dead-letter-exchange","stock-event-exchange");
        arguments.put("x-dead-letter-routing-key","stock.release");
        arguments.put("x-message-ttl",120000);
        Queue queue = new Queue("stock.delay.queue", true, false, false,arguments);
        return queue;
    }

    @Bean
    public Binding stockReleaseBinding(){
        Binding binding = new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE, "stock-event-exchange", "stock.release.#",null);
        return binding;
    }
    @Bean
    public Binding stockLockBinding(){
        Binding binding = new Binding("stock.delay.queue", Binding.DestinationType.QUEUE, "stock-event-exchange", "stock.locked",null);
        return binding;
    }


}
