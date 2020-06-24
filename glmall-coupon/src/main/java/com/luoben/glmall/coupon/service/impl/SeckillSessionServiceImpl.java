package com.luoben.glmall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.glmall.coupon.dao.SeckillSessionDao;
import com.luoben.glmall.coupon.entity.SeckillSessionEntity;
import com.luoben.glmall.coupon.entity.SeckillSkuRelationEntity;
import com.luoben.glmall.coupon.service.SeckillSessionService;
import com.luoben.glmall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取最近三天的秒杀活动
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getLates3DaySession() {
        //计算最近三天
        QueryWrapper<SeckillSessionEntity> wrapper = new QueryWrapper<>();
        wrapper.between("start_time",startTime(),endTime());

        List<SeckillSessionEntity> list = list(wrapper);

        if(list!=null&&list.size()>0){
            List<SeckillSessionEntity> collect = list.stream().map(sesssion -> {
                Long id = sesssion.getId();//当前活动场次id
                QueryWrapper<SeckillSkuRelationEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("promotion_session_id", id);
                List<SeckillSkuRelationEntity> skuRelationEntities = seckillSkuRelationService.list(queryWrapper);
                sesssion.setRelationSkus(skuRelationEntities);
                return sesssion;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }


    private String startTime(){
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime startTime = LocalDateTime.of(now, min);
        String format = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return  format;
    }

    private String endTime(){
        LocalDate now = LocalDate.now();
        LocalDate localDate = now.plusDays(2);
        LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
        String format = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return  format;
    }
}