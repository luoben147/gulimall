package com.luoben.glmall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luoben.common.utils.PageUtils;
import com.luoben.common.utils.Query;
import com.luoben.glmall.product.dao.AttrAttrgroupRelationDao;
import com.luoben.glmall.product.dao.AttrGroupDao;
import com.luoben.glmall.product.entity.AttrAttrgroupRelationEntity;
import com.luoben.glmall.product.entity.AttrEntity;
import com.luoben.glmall.product.entity.AttrGroupEntity;
import com.luoben.glmall.product.service.AttrGroupService;
import com.luoben.glmall.product.service.AttrService;
import com.luoben.glmall.product.vo.AttrGroupRelationVo;
import com.luoben.glmall.product.vo.AttrGroupWithAttrsVo;
import com.luoben.glmall.product.vo.SkuItemVo;
import com.luoben.glmall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Resource
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key)
                        .or()
                        .like("attr_group_name", key);
            });
        }

        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), new QueryWrapper<AttrGroupEntity>());
            return new PageUtils(page);
        } else {
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        }
    }



    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
//        relationDao.delete(new QueryWrapper<AttrAttrgroupRelationEntity>()
//                .eq("attr_id",1L)
//                .eq("attr_group_id",1L));
        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        relationDao.deleteBatchRelation(entities);

    }

    /**
     * 根据三级分类id 查出所有分组以及组里的所有属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        //1.查询当前分类下的所有属性分组
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("catelog_id",catelogId);
        List<AttrGroupEntity> attrGroupEntityList = this.list(queryWrapper);
        //2.查询每个分组下的所有属性
        List<AttrGroupWithAttrsVo> collect = attrGroupEntityList.stream().map(group -> {
            AttrGroupWithAttrsVo vo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group,vo);
            List<AttrEntity> attrs = attrService.getRelationAttr(vo.getAttrGroupId());
            vo.setAttrs(attrs);
            return vo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * spu 对应的 所有分组信息及其下的属性信息
     * @param spuId
     * @param catalogId
     * @return
     */
    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {

        List<SpuItemAttrGroupVo> vos= this.baseMapper.getAttrGroupWithAttrsBySpuId(spuId,catalogId);

        return vos;
    }
}