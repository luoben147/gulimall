package com.luoben.glmall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * RabbitMQ
 *  1. RabbitAutoConfiguration
 *  2.给容器中自动配置了
 *      RabbitTemplate 、AmqpAdmin、CachingConnectionFactory、RabbitMessagingTemplate
 *      所有属性在 RabbitProperties 中
 *
 *  3. @EnableRabbit
 *  4. 监听消息：
 *      @RabbitListener :类和方法上(监听哪些队列即可)  必须@EnableRabbit
 *      @RabbitHandler  :方法上（重载区分不同的消息）
 *
 *@RabbitListener:
 * queues: 声明需要监听的所有队列
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
 *  @RabbitListener(queues = {"hello-java-queue"})
 *  public void receiveMessage(Message message,
 *          OrderReturnReasonEntity content,
 *          Channel channel)throws InterruptedException{
 *                   //{"id":1,"name":"哈哈","sort":null,"status":null,"createTime":1592294151728}
 *          System.out.println("接受到消息.."+content);
 *          byte[]body=message.getBody();
 *                   //消息头 属性信息
 *          MessageProperties properties=message.getMessageProperties();
 *          Thread.sleep(3000);
 *          System.out.println("消息处理完成==>"+content.getName());
 *  }
 *
 */
@EnableRabbit
@EnableRedisHttpSession
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class GlmallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlmallOrderApplication.class, args);
    }

}
