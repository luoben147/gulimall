package com.luoben.glmall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luoben.common.utils.PageUtils;
import com.luoben.glmall.product.entity.CategoryEntity;
import com.luoben.glmall.product.vo.Catelog2VO;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author luoben
 * @email luoben@gmail.com
 * @date 2020-05-19 14:48:18
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> ids);

    /**
     * 找到catelogId 完整路径
     * [父/子/孙]
     * @param catelogId
     * @return
     */
    Long[] findCatelogPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    /**
     * 查询所有一级分类
     * @return
     */
    List<CategoryEntity> getLevelOneCategorys();

    Map<String, List<Catelog2VO>> getCatelogJson();
}

