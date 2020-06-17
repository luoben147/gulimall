package com.luoben.glmall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.member.entity.MemberLevelEntity;

import java.util.Map;

/**
 * 会员等级
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:33:53
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 获取默认的会员等级
     * @return
     */
    MemberLevelEntity getDefaultLevel();
}

