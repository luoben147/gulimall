package com.luoben.glmall.coupon.dao;

import com.luoben.glmall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:00:55
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
