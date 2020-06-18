package com.luoben.glmall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.exception.NoStockException;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.common.utils.R;
import com.luoben.common.vo.MemberResponseVO;
import com.luoben.glmall.order.constant.OrderConstant;
import com.luoben.glmall.order.dao.OrderDao;
import com.luoben.glmall.order.entity.OrderEntity;
import com.luoben.glmall.order.entity.OrderItemEntity;
import com.luoben.glmall.order.enume.OrderStatusEnum;
import com.luoben.glmall.order.feign.CartFeignService;
import com.luoben.glmall.order.feign.MemberFeignService;
import com.luoben.glmall.order.feign.ProductFeignService;
import com.luoben.glmall.order.feign.WareFeignService;
import com.luoben.glmall.order.interceptor.LoginUserInterceptor;
import com.luoben.glmall.order.service.OrderItemService;
import com.luoben.glmall.order.service.OrderService;
import com.luoben.glmall.order.to.OrderCreateTo;
import com.luoben.glmall.order.vo.*;
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
    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal=new InheritableThreadLocal<>();

    @Autowired
    OrderItemService orderItemService;

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
        OrderConfirmVo confirmVo=new OrderConfirmVo();
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        //在异步下 线程不同，会出现获取不到请求的问题 所有在异步情况下 重新设置请求
        System.out.println("主线程.."+Thread.currentThread().getId());
        //获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1.远程查询所有的收货地址列表
            System.out.println("member线程.."+Thread.currentThread().getId());
            //每一个线程都共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddressByMemberId(memberResponseVO.getId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2.远程查询购物车所有选中的购物项
            System.out.println("cart线程.."+Thread.currentThread().getId());
            //每一个线程都共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(()->{
            //继续查询商品有货无货
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R r = wareFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = r.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if(data!=null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        },executor);

        //3.查询用户优惠信息    用户积分
        Integer integration = memberResponseVO.getIntegration();
        confirmVo.setIntegration(integration);

        //4.其他数据 自动计算

        //TODO 5.防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberResponseVO.getId(),token,30,TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressFuture,cartFuture).get();

        return confirmVo;
    }

    /**
     * 提交订单功能
     * 高并发不适合使用seata AT模式分布式事务
     *  @GlobalTransactional
     * @param vo
     * @return
     */
    //@GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        submitVoThreadLocal.set(vo);
        SubmitOrderResponseVo response=new SubmitOrderResponseVo();
        response.setCode(0);
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();

        //1.验证令牌【令牌的对比和删除必须保证原子性】
        //0令牌失败   1删除成功
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        //原子验证令牌和删除令牌
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVO.getId()), orderToken);
        if(execute==0L){
            //令牌验证失败
            response.setCode(1);
            return response;
        }else {
            //令牌验证成功
            //下单：创建订单，验令牌，验价格，锁库存..
            //1.创建订单
            OrderCreateTo order = createOrder();
            //2.验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
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
                if(r.getCode()==0){
                    //锁库存成功
                    response.setOrder(order.getOrder());

                    //TODO 5.远程扣减积分 异常
                    int i=10/0;
                    return response;
                }else {
                    //锁库存失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            }else {
                response.setCode(2);
                return response;
            }
        }

//        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVO.getId());
//        if(orderToken!=null&&orderToken.equals(redisToken)){
//            //令牌验证通过
//            redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVO.getId());
//        }else {
//            //不通过
//
//        }
    }

    /**
     * 保存订单数据
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
     * @return
     */
    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1.生成订单号
        String orderSn = IdWorker.getTimeId();
        //构建订单信息
        OrderEntity orderEntity = buildOrder(orderSn);

        //2.获取所有订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        //3.计算价格,积分相关
        computePrice(orderEntity,itemEntities);

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
            promotion=promotion.add(entity.getPromotionAmount());//商品促销分解金额
            coupon = coupon.add(entity.getCouponAmount());//优惠券优惠分解金额
            integration = integration.add(entity.getIntegrationAmount());//积分优惠分解金额
            total=total.add(entity.getRealAmount());//该商品经过优惠后的分解金额
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
     * @param orderSn
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();
        OrderEntity entity=new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(memberResponseVO.getId());

        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        //获取收货地址
        R fare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if(fare.getCode()==0){
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
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems!=null&&currentUserCartItems.size()>0){
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