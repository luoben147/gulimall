package com.luoben.glmall.product.dao;

import com.luoben.glmall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 14:48:18
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    /**
     * 修改spu发布状态
     * @param spuId
     * @param code
     */
    void updateSpuStatus(@Param("spuId") Long spuId,@Param("code") int code);
}
