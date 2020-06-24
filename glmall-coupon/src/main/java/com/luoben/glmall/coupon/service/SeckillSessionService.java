package com.luoben.glmall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.coupon.entity.SeckillSessionEntity;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动场次
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:00:55
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 获取最近三天的秒杀活动
     * @return
     */
    List<SeckillSessionEntity> getLates3DaySession();
}

