package com.luoben.glmall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.glmall.product.dao.SkuSaleAttrValueDao;
import com.luoben.glmall.product.entity.SkuSaleAttrValueEntity;
import com.luoben.glmall.product.service.SkuSaleAttrValueService;
import com.luoben.glmall.product.vo.SkuItemSaleAttrVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * spu 销售属性所有组合
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId) {
        List<SkuItemSaleAttrVo> saleAttrVos = this.baseMapper.getSaleAttrsBySpuId(spuId);
        return saleAttrVos;
    }

    @Override
    public List<String> getSkuSaleAttrValuesAsStringList(Long skuId) {

        return this.baseMapper.getSkuSaleAttrValuesAsStringList(skuId);
    }

}