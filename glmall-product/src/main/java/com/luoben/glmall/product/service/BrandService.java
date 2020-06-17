package com.luoben.glmall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.product.entity.BrandEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 14:48:18
 */
public interface BrandService extends IService<BrandEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void updateDetail(BrandEntity brand);

    /**
     * 根据id集合 获取对应的品牌信息
     * @param brandIds
     * @return
     */
    List<BrandEntity> getBrandsByIds(List<Long> brandIds);
}

