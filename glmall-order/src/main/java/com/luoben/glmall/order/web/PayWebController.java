package com.luoben.glmall.order.web;

import com.alipay.api.AlipayApiException;
import com.luoben.glmall.order.config.AlipayTemplate;
import com.luoben.glmall.order.service.OrderService;
import com.luoben.glmall.order.vo.PayVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {

    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    /**
     * 1.将支付页让浏览器展示
     * 2.支付成功以后，跳转到用户的订单列表
     * @param orderSn
     * @return
     * @throws AlipayApiException
     */
    @ResponseBody
    @GetMapping(value = "/payOrder",produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        System.out.println("去支付订单号："+orderSn);
        PayVo payVo=orderService.getOrderPay(orderSn);
        //返回的是一个页面，将此页面直接返回给浏览器
        String pay = alipayTemplate.pay(payVo);
        System.out.println(pay);
        return pay;
    }


    @PostMapping("/queryPayStatus")
    @ResponseBody
    public String queryPayStatus(@RequestParam("orderSn") String orderSn){
        String result=orderService.queryPayStatus(orderSn);
        if(!StringUtils.isEmpty(result)){
            return result;
        }else {
            return "queryPayStatus error";
        }
    }


}
