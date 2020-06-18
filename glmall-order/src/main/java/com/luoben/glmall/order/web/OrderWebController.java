package com.luoben.glmall.order.web;

import com.luoben.common.exception.NoStockException;
import com.luoben.glmall.order.service.OrderService;
import com.luoben.glmall.order.vo.OrderConfirmVo;
import com.luoben.glmall.order.vo.OrderSubmitVo;
import com.luoben.glmall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    //跳转 结算页面
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo = orderService.confirmOrder();

        //展示订单确认的数据
        model.addAttribute("data", confirmVo);
        return "confirm";
    }

    /**
     * 下单
     *
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {

        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            if (responseVo.getCode() == 0) {
                //下单成功==> 支付选择页
                model.addAttribute("submitOrderResp", responseVo);
                return "pay";
            } else {
                String msg = "下单失败；";
                switch (responseVo.getCode()) {
                    case 1:
                        msg += "订单信息过期，请刷新再次提交";
                        break;
                    case 2:
                        msg += "订单商品价格发生变化，请确认后再次提交";
                        break;
                    case 3:
                        msg += "库存锁定失败，商品库存不足";
                        break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                //下单失败回到订单确认页，重新确认订单信息
                return "redirect:http://order.glmall.com/toTrade";
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(e instanceof NoStockException){
                String message=((NoStockException)e).getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.glmall.com/toTrade";
        }
    }

}
