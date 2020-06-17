package com.luoben.glmall.member.dao;

import com.luoben.glmall.member.entity.MemberLevelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级
 * 
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 16:33:53
 */
@Mapper
public interface MemberLevelDao extends BaseMapper<MemberLevelEntity> {

    /**
     * 获取默认的会员等级
     * @return
     */
    MemberLevelEntity getDefaultLevel();
}
