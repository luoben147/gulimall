package com.luoben.glmall.order.dao;

import com.luoben.glmall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:47:07
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
