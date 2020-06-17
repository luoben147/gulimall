package com.luoben.glmall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.product.entity.AttrEntity;
import com.luoben.glmall.product.vo.AttrRespVo;
import com.luoben.glmall.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 14:48:18
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    List<AttrEntity> getRelationAttr(Long attrgroupId);


    PageUtils getNoRelationAttr(Map<String,Object> params, Long attrgroupId);

    /**
     * 在指定的所有属性集合中，挑出检索属性
     * @param attrIds
     * @return
     */
    List<Long> selectSearchAttrIds(List<Long> attrIds);
}

