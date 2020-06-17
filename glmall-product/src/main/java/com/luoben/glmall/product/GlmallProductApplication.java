package com.luoben.glmall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1、整合MyBatis-Plus
 *      1）、导入依赖
 *      <dependency>
 *             <groupId>com.baomidou</groupId>
 *             <artifactId>mybatis-plus-boot-starter</artifactId>
 *             <version>3.2.0</version>
 *      </dependency>
 *      2）、配置
 *          1、配置数据源；
 *              1）、导入数据库的驱动。https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-versions.html
 *              2）、在application.yml配置数据源相关信息
 *          2、配置MyBatis-Plus；
 *              1）、使用@MapperScan
 *              2）、告诉MyBatis-Plus，sql映射文件位置
 *
 * 2、逻辑删除
 *  1）、配置全局的逻辑删除规则（省略）
 *  2）、给Bean加上逻辑删除注解@TableLogic（value,delval）
 *      value； 逻辑未删除值
 *      delval： 逻辑删除值
 *
 * 3、JSR303
 *   1）、给Bean添加校验注解:javax.validation.constraints，并定义自己的message提示
 *   2)、开启校验功能@Valid
 *      效果：校验错误以后会有默认的响应；
 *   3）、给校验的bean后紧跟一个BindingResult，就可以获取到校验的结果
 *   4）、分组校验（多场景的复杂校验）
 *         1)、	@NotBlank(message = "品牌名必须提交",groups = {AddGroup.class,UpdateGroup.class})
 *          给校验注解标注什么情况需要进行校验
 *         2）、@Validated({AddGroup.class})
 *         3)、默认没有指定分组的校验注解@NotBlank，在分组校验情况@Validated({AddGroup.class})下不生效，只会在@Validated生效；
 *
 *   5）、自定义校验
 *      1）、编写一个自定义的校验注解
 *      2）、编写一个自定义的校验器 ConstraintValidator
 *      3）、关联自定义的校验器和自定义的校验注解
 *      @Documented
 * @Constraint(validatedBy = { ListValueConstraintValidator.class【可以指定多个不同的校验器，适配不同类型的校验】 })
 * @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
 * @Retention(RUNTIME)
 * public @interface ListValue {
 *
 * 4、统一的异常处理
 * @ControllerAdvice
 *  1）、编写异常处理类，使用@ControllerAdvice。
 *  2）、使用@ExceptionHandler标注方法可以处理的异常。
 *
 * 5、nginx动静分离
 *     静态资源放在nginx的html下的static文件夹下
 *     页面放在各微服务的templates文件夹下
 *
 * 6 、整合redis 缓存数据
 *     引入data-redis-starter
 *     yml配置host等信息
 *     Springboot自动配置好的StringRedisTemplate 操作redis
 *
 * 7、整合redisson 分布式锁
 * 8、整合SpringCache 简化缓存开发
 *      spring-boot-starter-cache,spring-boot-starter-data-redis
 *      自动配置：RedisCacheConfiguration
 *      2.使用spring.cache.type=redis 配置redis作为缓存
 *      @Cacheable      : 触发将数据保存到缓存的操作
 *      @CacheEvict     : 触发将数据从缓存删除
 *      @CachePut       ：不影响方法执行更新缓存
 *      @Caching        : 组合以上多个操作
 *      @CacheConfig    : 在类级别共享缓存的配置
 *
 *      3.开启缓存 @EnableCaching
 *      4.Spring-Cache不足：
 *         1）、读模式：
 *             缓存穿透：查询一个null数据。解决：缓存空数据 spring.cache.redis.cache-null-values: true
 *             缓存击穿：大量并发进来同时查询一个正好过期的数据。解决：加锁; ？ 默认没有加锁 @Cacheable(sync = true) 加本地锁解决缓存击穿
 *             缓存雪崩：大量的key同时过期。解决：加随机时间。加上过期时间。 spring.cache.redis.time-to-live=3600s
 *         2）、写模式（缓存与数据库一致）
 *              1）、读写加锁
 *              2）、引入Canal,感知mysql的更新去更新数据库
 *              3）、读多写多，直接去查数据库
 *       总结：
 *          常规数据：（读多写少，即时性，一致性要求不高的数据），完全可以使用Spring-Cache
 *                  写模式（只要缓存的数据有过期时间就足够了）
 *          特殊数据：特殊设计
 *      原理：
 *          CacheManager(RedisCacheManager)->Cache(RedisCache)->Cache负责缓存读写
 *
 */

@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.luoben.glmall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.luoben.glmall.product.dao")
@SpringBootApplication
public class GlmallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlmallProductApplication.class, args);
    }

}
