package com.luoben.glmall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * 基本属性分组
 */
@Data
public class SpuItemAttrGroupVo {
    private String groupName;   //所属分组
    private List<Attr> attrs;
}
