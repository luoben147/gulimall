package com.luoben.glmall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
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
 *  本地事务失败问题
 *   同一个对象内事务方法互相调用默认事务是无效的，原因是绕过了代理对象，事务是使用代理对象来控制的
 *   解决方法：使用代理对象来调用事务方法
 *    1)引入aop-starter;spring-starter-aop,引入aspectj
 *    2）@EnableAspectJAutoProxy开启aspectj动态代理功能，以后所有的动态代理都是aspectj创建的（即使没有接口也可以创建动态代理）
 *    exposeProxy = true对外暴露代理对象
 *    3）本类方法调用使用代理对象调用
 *       OrderServiceImpl OrderService = (OrderServiceImpl)AopContext.currentProxy();
 *
 *
 * Seata控制分布式事务
 *   1)、每一个微服务先必须创建undo_ ,log;
 *   2)、安装事务协调器; seata-server: https ://qithub. com/seata/seata/releases
 *   3)、整合
 *        1、导入依赖spring-cloud-starter-alibaba-seata  seata-all-1.0.0
 *       2、解压并启动seata-server;
 *           registry.conf:注册中心配置;修改registry type=nacos
 *           file. conf:
 *       3、所有想要用到分布式事务的微服务使用seata DataSourceProxy代理自己的数据源
 *       4、每个微服务，都必须导入
 *           registry.conf
 *           file.conf vgroup_ mapping. {application. name}-fescar-service-group = "default"
 *       5、启动测试分布式事务
 *       6、给分布式大事务的入口标注@Global Transactional
 *       7、每一个远程的小事务用 @Transactional
 *
 *   在高并发情况下 不适用 Seata的AT模式（默认）
 *
 * 两阶段提交协议的演变：
 *     一阶段：业务数据和回滚日志记录在同一个本地事务中提交，释放本地锁和连接资源。
 *     二阶段：
 *         提交异步化，非常快速地完成。
 *         回滚通过一阶段的回滚日志进行反向补偿。
 * 它会加很多锁，把并发变成串行化，这在高并发场景下不适用
 *
 * 在高并发情况下，大多采用消息模式下的
 *      最大努力通知型方案
 *      可靠消息+最终一致性方案（异步确保型）
 */
@EnableAspectJAutoProxy(exposeProxy = true)
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
