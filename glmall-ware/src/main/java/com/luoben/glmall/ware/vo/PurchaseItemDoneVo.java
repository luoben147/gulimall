package com.luoben.glmall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {

    /**
     * 采购项id
     */
    private Long itemId;

    /**
     * 完成状态
     */
    private Integer status;

    /**
     * 原因
     */
    private String reason;
}
