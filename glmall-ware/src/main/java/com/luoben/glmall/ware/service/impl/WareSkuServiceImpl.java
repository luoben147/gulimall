package com.luoben.glmall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.exception.NoStockException;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.common.utils.R;
import com.luoben.glmall.ware.dao.WareSkuDao;
import com.luoben.glmall.ware.entity.WareSkuEntity;
import com.luoben.glmall.ware.feign.ProductFeignService;
import com.luoben.glmall.ware.service.WareSkuService;
import com.luoben.glmall.ware.vo.OrderItemVo;
import com.luoben.glmall.ware.vo.SkuHasStockVo;
import com.luoben.glmall.ware.vo.WareSkuLockVo;
import lombok.Data;
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

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id",skuId);
        }

        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id",wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 添加商品库存
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断如果没有这个库存记录 新增
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id",skuId);
        wrapper.eq("ware_id",wareId);
        List<WareSkuEntity> entityList = wareSkuDao.selectList(wrapper);
        if(CollectionUtils.isEmpty(entityList)){
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
                if(info.getCode()==0){
                    Map<String,Object> data = (Map<String,Object>)info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            wareSkuDao.insert(wareSkuEntity);
        }else {
            wareSkuDao.addStock(skuId,wareId,skuNum);
        }
    }

    /**
     * 查询sku是否有库存
     * @param skuIds
     * @return
     */
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //查询当前sku的总库存量
            Long count= baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count==null?false:count>0);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * 为订单锁定库存
     *
     * 默认只要是运行时异常都会回滚
     * @param lockVo
     * @return
     */
    @Transactional(rollbackFor =NoStockException.class )
    @Override
    public Boolean orderLockStock(WareSkuLockVo lockVo) {

        //1.按照下单的收货地址，找到一个就近仓库，锁定库存

        //1.找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = lockVo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询商品在哪里有库存
            List<Long> wareIds= baseMapper.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        Boolean allLock=true;
        //2.锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked=false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if(wareIds==null||wareIds.size()==0){
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                //成功返回1 ，否则返回0
               Long count= baseMapper.lockSkuStock(skuId,wareId,hasStock.getNum());
                if(count==1){
                    //当前仓库锁定商品成功，跳出，其他仓库就不需要锁定
                    skuStocked=true;
                    break;
                }else {
                    //当前仓库锁定失败，重试下一个仓库
                }
            }
            if(!skuStocked){
                //当前商品 所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        //3.全部商品锁定成功
        return true;
    }

    @Data
    class SkuWareHasStock{
        private Long skuId; //商品id
        private Integer num; //商品数量
        private List<Long> wareId;//仓库id

    }

}