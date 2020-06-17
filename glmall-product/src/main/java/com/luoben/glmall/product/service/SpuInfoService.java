package com.luoben.glmall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.product.entity.SpuInfoEntity;
import com.luoben.glmall.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 14:48:18
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String,Object> params);

    /**
     * 商品上架
     * @param spuId
     */
    void up(Long spuId);

    /**
     * skuId 查询 Spu信息
     * @return
     */
    SpuInfoEntity getSpuInfoBySkuId(Long skuId);
}

