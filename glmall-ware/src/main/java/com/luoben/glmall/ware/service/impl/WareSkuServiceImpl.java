package com.luoben.glmall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.exception.NoStockException;
import com.luoben.common.to.mq.OrderTo;
import com.luoben.common.to.mq.StockDetailTo;
import com.luoben.common.to.mq.StockLockedTo;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.common.utils.R;
import com.luoben.glmall.ware.dao.WareSkuDao;
import com.luoben.glmall.ware.entity.WareOrderTaskDetailEntity;
import com.luoben.glmall.ware.entity.WareOrderTaskEntity;
import com.luoben.glmall.ware.entity.WareSkuEntity;
import com.luoben.glmall.ware.feign.OrderFeignService;
import com.luoben.glmall.ware.feign.ProductFeignService;
import com.luoben.glmall.ware.service.WareOrderTaskDetailService;
import com.luoben.glmall.ware.service.WareOrderTaskService;
import com.luoben.glmall.ware.service.WareSkuService;
import com.luoben.glmall.ware.vo.OrderItemVo;
import com.luoben.glmall.ware.vo.OrderVo;
import com.luoben.glmall.ware.vo.SkuHasStockVo;
import com.luoben.glmall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Resource
    WareSkuDao wareSkuDao;

    @Resource
    ProductFeignService productFeignService;

    @Resource
    OrderFeignService orderFeignService;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 添加商品库存
     *
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断如果没有这个库存记录 新增
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId);
        wrapper.eq("ware_id", wareId);
        List<WareSkuEntity> entityList = wareSkuDao.selectList(wrapper);
        if (CollectionUtils.isEmpty(entityList)) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程查询sku名称 ,如果失败，整个事务无需回滚
            //1、 自己catch异常
            //TODO 还可以用什么办法让异常出现以后不回滚？高级
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            wareSkuDao.insert(wareSkuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    /**
     * 查询sku是否有库存
     *
     * @param skuIds
     * @return
     */
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //查询当前sku的总库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * 为订单锁定库存
     * 默认只要是运行时异常都会回滚
     * <p>
     * 库存解锁的场景
     * 1）下订单成功，订单过期没有支付被系统自动取消、被用户手动取消都要解锁库存
     * 2）下订单成功，库存锁定成功，接下的业务调用失败，导致订单回滚 之前锁定的库存就要自动解锁
     *
     * @param lockVo
     * @return
     */
    @Transactional()
    @Override
    public Boolean orderLockStock(WareSkuLockVo lockVo) {

        /**
         * 保存库存工作单详情，追溯哪个仓库锁定了多少，一旦出问题，人为回滚
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(lockVo.getOrderSn());
        wareOrderTaskService.save(taskEntity);


        //1.按照下单的收货地址，找到一个就近仓库，锁定库存
        //1.找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = lockVo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询商品在哪里有库存
            List<Long> wareIds = baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        Boolean allLock = true;
        //2.锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }

            //1.如果每一件商品都锁定成功，将当前商品锁定了的工作单记录发送给MQ
            //2.锁定失败。前面保存的工作单信息就回滚了。发送出去的消息，即使要解锁记录，由于在数据库查不到id，所有就不用解锁
            for (Long wareId : wareIds) {
                //成功返回1 ，否则返回0
                Long count = baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    //当前仓库锁定商品成功，跳出，其他仓库就不需要锁定
                    skuStocked = true;
                    //TODO　告诉MQ 库存锁定成功
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
                    taskDetailEntity.setSkuId(skuId);
                    taskDetailEntity.setSkuName("");
                    taskDetailEntity.setSkuNum(hasStock.getNum());
                    taskDetailEntity.setTaskId(taskEntity.getId());
                    taskDetailEntity.setWareId(wareId);
                    taskDetailEntity.setLockStatus(1);
                    wareOrderTaskDetailService.save(taskDetailEntity);

                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);

                    //库存锁定成功，发送延时消息到死信队列    过期后执行解锁库存逻辑
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                } else {
                    //当前仓库锁定失败，重试下一个仓库
                }
            }
            if (!skuStocked) {
                //当前商品 所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        //3.全部商品锁定成功
        return true;
    }


    //解锁库存
    @Override
    public void unlockStock(StockLockedTo to){
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();
        //解锁
        //查询数据库关于这个订单的锁定库存信息
        //  ①有。证明库存锁定成功了
        //          解锁：订单情况
        //          1、没有这个订单，必须解锁
        //          2、有这个订单，不是解锁库存
        //              订单状态：已取消：解锁库存
        //                        没有取消，不能解锁
        //  ②没有：库存锁定失败了，库存回滚了，这个情况无需解锁
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null) {
            //有，解锁
            Long id = to.getId();//库存工作单的id
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();//根据订单号查询订单状态
            R r = orderFeignService.getOrderByOrderSn(orderSn);
            if (r.getCode() == 0) {
                //订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });

                if (data == null || data.getStatus() == 4) {
                    //订单不存在 或者订单已经被取消了，才能解锁库存
                    if (byId.getLockStatus() == 1) {
                        //当前库存工作单详情，状态为1：已锁定但是未解锁才可以解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            }else {
                throw new RuntimeException("远程服务失败");
            }
        }else{
           //没有，无需解锁
        }
    }


    //防止订单服务卡顿，导致订单状态一直改不了，库存消息优先到期，查询订单状态是新建状态，什么都不做
    //导致卡顿的订单，永远不能解锁库存
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查询最新的库存的状态，防止重复解锁库存
        WareOrderTaskEntity taskEntity= wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = taskEntity.getId();
        //按库存工作单id 查询所有未解锁的库存，进行解锁
        QueryWrapper<WareOrderTaskDetailEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id",id);
        wrapper.eq("lock_status",1);
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(wrapper);

        for (WareOrderTaskDetailEntity entity : list) {
            unLockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }
    }


    /**
     * 解锁库存
     * @param skuId
     * @param wareId
     * @param num
     * @param taskDetailId
     */
    public void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        //库存解锁
        baseMapper.unLockStock(skuId, wareId, num);
        //更新库存工作单的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);//变为已解锁
        wareOrderTaskDetailService.updateById(entity);
    }


    @Data
    class SkuWareHasStock {
        private Long skuId; //商品id
        private Integer num; //商品数量
        private List<Long> wareId;//仓库id

    }

}