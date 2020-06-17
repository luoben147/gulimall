package com.luoben.glmall.order.web;

import com.luoben.glmall.order.service.OrderService;
import com.luoben.glmall.order.vo.OrderConfirmVo;
import com.luoben.glmall.order.vo.OrderSubmitVo;
import com.luoben.glmall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    //跳转 结算页面
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo=orderService.confirmOrder();

        //展示订单确认的数据
        model.addAttribute("data",confirmVo);
        return "confirm";
    }

    /**
     * 下单
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo){

        SubmitOrderResponseVo responseVo= orderService.submitOrder(vo);

        if(responseVo.getCode()==0){
            //下单成功==> 支付选择页

            return "pay";
        }else {
            //下单失败回到订单确认页，重新确认订单信息
            return "redirect:http://order.glmall.com/toTrade";
        }
    }

}
