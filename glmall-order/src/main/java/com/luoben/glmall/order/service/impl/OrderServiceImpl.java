package com.luoben.glmall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.exception.NoStockException;
import com.luoben.common.to.mq.OrderTo;
import com.luoben.common.to.mq.SeckillOrderTo;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.common.utils.R;
import com.luoben.common.vo.MemberResponseVO;
import com.luoben.glmall.order.config.AlipayTemplate;
import com.luoben.glmall.order.constant.OrderConstant;
import com.luoben.glmall.order.dao.OrderDao;
import com.luoben.glmall.order.entity.OrderEntity;
import com.luoben.glmall.order.entity.OrderItemEntity;
import com.luoben.glmall.order.entity.PaymentInfoEntity;
import com.luoben.glmall.order.enume.OrderStatusEnum;
import com.luoben.glmall.order.feign.CartFeignService;
import com.luoben.glmall.order.feign.MemberFeignService;
import com.luoben.glmall.order.feign.ProductFeignService;
import com.luoben.glmall.order.feign.WareFeignService;
import com.luoben.glmall.order.interceptor.LoginUserInterceptor;
import com.luoben.glmall.order.service.OrderItemService;
import com.luoben.glmall.order.service.OrderService;
import com.luoben.glmall.order.service.PaymentInfoService;
import com.luoben.glmall.order.to.OrderCreateTo;
import com.luoben.glmall.order.vo.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    //共享数据      每个提交订单请求携带的数据   保证线程数据安全
    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new InheritableThreadLocal<>();

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    PaymentInfoService paymentInfoService;

    @Resource
    MemberFeignService memberFeignService;

    @Resource
    CartFeignService cartFeignService;

    @Resource
    WareFeignService wareFeignService;

    @Resource
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        //在异步下 线程不同，会出现获取不到请求的问题 所有在异步情况下 重新设置请求
        System.out.println("主线程.." + Thread.currentThread().getId());
        //获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1.远程查询所有的收货地址列表
            System.out.println("member线程.." + Thread.currentThread().getId());
            //每一个线程都共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddressByMemberId(memberResponseVO.getId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2.远程查询购物车所有选中的购物项
            System.out.println("cart线程.." + Thread.currentThread().getId());
            //每一个线程都共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            //继续查询商品有货无货
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R r = wareFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = r.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);

        //3.查询用户优惠信息    用户积分
        Integer integration = memberResponseVO.getIntegration();
        confirmVo.setIntegration(integration);

        //4.其他数据 自动计算

        //TODO 5.防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVO.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();

        return confirmVo;
    }

    /**
     * 提交订单功能
     * 高并发不适合使用seata AT模式分布式事务
     *
     * @param vo
     * @return
     * @GlobalTransactional
     */
    //@GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        submitVoThreadLocal.set(vo);
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        response.setCode(0);
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        //1.验证令牌【令牌的对比和删除必须保证原子性】
        //0令牌失败   1删除成功
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        //原子验证令牌和删除令牌
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVO.getId()), orderToken);
        if (execute == 0L) {
            //令牌验证失败
            response.setCode(1);
            return response;
        } else {
            //令牌验证成功
            //下单：创建订单，验令牌，验价格，锁库存..
            //1.创建订单
            OrderCreateTo order = createOrder();
            //2.验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //金额对比成功

                //TODO 3.保存订单
                saveOrder(order);
                //4.库存锁定    只要有异常回滚订单数据
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());

                List<OrderItemVo> locks = order.getItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(locks);

                //TODO　远程锁定库存
                //库存成了，但是网络原因超时了，导致订单回滚，库存不回滚

                //为了保证高并发，库存服务自己回滚。可以发消息给库存服务;
                //库存服务本身也可以使用自动解锁模式     消息

                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0) {
                    //锁库存成功
                    response.setOrder(order.getOrder());

                    //TODO 5.远程扣减积分 异常
                    //int i=10/0;

                    //TODO 订单创建成功 发送延时消息给mq 死信队列    过期未支付 关闭订单
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());

                    return response;
                } else {
                    //锁库存失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            } else {
                response.setCode(2);
                return response;
            }
        }

    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        QueryWrapper<OrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("order_sn", orderSn);
        OrderEntity one = this.getOne(wrapper);
        return one;
    }

    /**
     * 关闭订单
     *
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        //查询当前订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            //关单
            OrderEntity update = new OrderEntity();
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);

            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            try {
                //TODO 保证消息一定会发送出去，，每一个消息都可以做好日志记录，（给数据库保存每一个消息的详细信息）
                //TODO 定期扫描数据库将失败的消息再发送一遍
                //发送订单关单消息给mq  库存服务（其他服务）监听消息 执行解锁库存
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (AmqpException e) {
                //TODO 将没发送成功的消息进行重试发送
                e.printStackTrace();
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();

        OrderEntity order = this.getOrderByOrderSn(orderSn);
        //订单金额，保留2位小数向上取值
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString()); //订单金额
        payVo.setOut_trade_no(orderSn);//订单号

        QueryWrapper<OrderItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("order_sn",orderSn);
        List<OrderItemEntity> list = orderItemService.list(wrapper);
        OrderItemEntity itemEntity = list.get(0);

        payVo.setSubject(itemEntity.getSkuName()); //订单的主题
        payVo.setBody(itemEntity.getSkuAttrsVals());//订单备注
        return payVo;
    }


    /**
     * 分页查询当前登录用户的所有订单信息
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",memberResponseVO.getId()).orderByDesc("id")
        );

        List<OrderEntity> collect = page.getRecords().stream().map(order -> {

            QueryWrapper<OrderItemEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("order_sn",order.getOrderSn());

            List<OrderItemEntity> itemEntities = orderItemService.list(wrapper);
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(collect);

        return new PageUtils(page);
    }

    /**
     * 处理支付宝异步通知结果
     * @param vo
     * @return
     */
    @Override
    public String handPayResult(PayAsyncVo vo) {

        //1.保存交易流水
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setAlipayTradeNo(vo.getTrade_no());
        paymentInfoEntity.setOrderSn(vo.getOut_trade_no());
        paymentInfoEntity.setPaymentStatus(vo.getTrade_status());
        paymentInfoEntity.setCallbackTime(vo.getNotify_time());

        paymentInfoService.save(paymentInfoEntity);

        //2.修改订单状态信息
        if(vo.getTrade_status().equals("TRADE_SUCCESS")||vo.getTrade_status().equals("TRADE_FINISHED")){
            //支付成功
            String outTradeNo = vo.getOut_trade_no();
            baseMapper.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    /**
     * 支付宝关闭支付订单
     * @param entity
     */
    @Override
    public void alipayTradeClose(OrderEntity entity) {

        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(alipayTemplate.getGatewayUrl(), alipayTemplate.getApp_id(), alipayTemplate.getMerchant_private_key(), "json", alipayTemplate.getCharset(), alipayTemplate.getAlipay_public_key(), alipayTemplate.getSign_type());

        //设置请求参数
        AlipayTradeCloseRequest alipayRequest = new AlipayTradeCloseRequest();

        //商户订单号，商户网站订单系统中唯一订单号
        String orderSn = entity.getOrderSn();
        System.out.println("关单订单号："+orderSn);
        //支付宝交易号
        //String trade_no = new String(request.getParameter("WIDTCtrade_no").getBytes("ISO-8859-1"),"UTF-8");
        //请二选一设置

        //alipayRequest.setBizContent("{\"out_trade_no\":\""+ orderSn +"\"," +"\"trade_no\":\""+ trade_no +"\"}");
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ orderSn+"\"}");

        //请求
        try {
            AlipayTradeCloseResponse response = alipayClient.execute(alipayRequest);
            if(response.isSuccess()){
                System.out.println("调用关单成功");
            } else {
                System.out.println("调用关单失败");

            }
            String body = response.getBody();
            System.out.println("关单结果："+body);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String queryPayStatus(String orderSn) {

        //获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(alipayTemplate.getGatewayUrl(), alipayTemplate.getApp_id(), alipayTemplate.getMerchant_private_key(), "json", alipayTemplate.getCharset(), alipayTemplate.getAlipay_public_key(), alipayTemplate.getSign_type());
        //设置请求参数
        AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
        //商户订单号，商户网站订单系统中唯一订单号
        //String out_trade_no = new String(request.getParameter("WIDTQout_trade_no").getBytes("ISO-8859-1"),"UTF-8");
        //支付宝交易号
        //String trade_no = new String(request.getParameter("WIDTQtrade_no").getBytes("ISO-8859-1"),"UTF-8");
        //请二选一设置

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ orderSn +"\"}");
        //请求
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(alipayRequest);
            if(response.isSuccess()){
                switch (response.getTradeStatus()) // 判断交易结果
                {
                    case "TRADE_FINISHED": // 交易结束并不可退款
                        break;
                    case "TRADE_SUCCESS": // 交易支付成功
                        break;
                    case "TRADE_CLOSED": // 未付款交易超时关闭或支付完成后全额退款
                        break;
                    case "WAIT_BUYER_PAY": // 交易创建并等待买家付款
                        break;
                    default:
                        break;
                }
                System.out.println("查询支付宝支付状态结果："+response.getTradeStatus());
                return response.getTradeStatus();
            }else {
                System.out.println("==================调用支付宝查询接口失败！");
                return null;
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建秒杀订单
     * @param seckillOrderTo
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrderTo) {
        //TODO 保存秒杀订单
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderEntity.setMemberId(seckillOrderTo.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = seckillOrderTo.getSeckillPrice().multiply(new BigDecimal("" + seckillOrderTo.getNum()));
        orderEntity.setPayAmount(multiply);
        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        orderItemEntity.setSkuQuantity(seckillOrderTo.getNum());
        //TODO 获取当前sku的详细信息进行设置          productFeignService。getSpuInfoBySkuId()
        orderItemService.save(orderItemEntity);


    }


    /**
     * 保存订单数据
     *
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getItems();
        orderItemService.saveBatch(orderItems);
    }


    /**
     * 创建订单
     *
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1.生成订单号
        String orderSn = IdWorker.getTimeId();
        //构建订单信息
        OrderEntity orderEntity = buildOrder(orderSn);

        //2.获取所有订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        //3.计算价格,积分相关
        computePrice(orderEntity, itemEntities);

        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setItems(itemEntities);
        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        //1.订单价格相关
        //订单总额=叠加每个订单项总额
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        for (OrderItemEntity entity : itemEntities) {
            promotion = promotion.add(entity.getPromotionAmount());//商品促销分解金额
            coupon = coupon.add(entity.getCouponAmount());//优惠券优惠分解金额
            integration = integration.add(entity.getIntegrationAmount());//积分优惠分解金额
            total = total.add(entity.getRealAmount());//该商品经过优惠后的分解金额
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString())); //赠送积分
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));//赠送成长值
        }
        //订单总额
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount())); //应付总额  订单总额+运费
        orderEntity.setPromotionAmount(promotion); //促销优化金额
        orderEntity.setCouponAmount(coupon);//优惠券抵扣金额
        orderEntity.setIntegrationAmount(integration);  //积分抵扣金额

        //积分
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
    }

    /**
     * 构建订单
     *
     * @param orderSn
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(memberResponseVO.getId());

        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        //获取收货地址
        R fare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (fare.getCode() == 0) {
            FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
            });
            //设置运费信息
            entity.setFreightAmount(fareResp.getFare());
            //设置收货人信息
            entity.setReceiverCity(fareResp.getAddress().getCity());
            entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
            entity.setReceiverName(fareResp.getAddress().getName());
            entity.setReceiverPhone(fareResp.getAddress().getPhone());
            entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
            entity.setReceiverProvince(fareResp.getAddress().getProvince());
            entity.setReceiverRegion(fareResp.getAddress().getRegion());
        }

        //设置订单相关状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);   //自动确认收货时间
        entity.setDeleteStatus(0); //未删除
        return entity;
    }

    /**
     * 构建所有订单项信息
     *
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());

            return itemEntities;
        }
        return null;
    }

    /**
     * 构建一个订单项
     *
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {

        OrderItemEntity itemEntity = new OrderItemEntity();
        //1.订单信息，订单号

        //2.商品spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfoVo.getId());
        itemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        itemEntity.setSpuName(spuInfoVo.getSpuName());
        itemEntity.setCategoryId(spuInfoVo.getCatalogId());

        //3.商品sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        //4.优惠信息

        //5.积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //6.订单项的价格信息
        // 商品促销分解金额
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        //优惠券优惠分解金额
        itemEntity.setCouponAmount(new BigDecimal("0"));
        //积分优惠分解金额
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //该商品经过优惠后的分解金额(当前订单项的实际金额)
        //总额 单价*数量
        BigDecimal orign = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        //总额-促销金额-优惠卷金额-积分优惠=支付金额
        BigDecimal subtract = orign.subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getCouponAmount()).subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);

        return itemEntity;
    }

}