package com.luoben.glmall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class PurchaseDoneVo {

    @NotNull
    private Long id; //采购单id

    /**
     * 采购项集合
     */
    private List<PurchaseItemDoneVo> items;

}
