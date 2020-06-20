package com.luoben.glmall.member.web;

import com.luoben.common.utils.R;
import com.luoben.glmall.member.feign.OrderFeignService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Resource
    OrderFeignService orderFeignService;

    /**
     * 订单分页查询
     * @param pageNum
     * @param model
     * @return
     */
    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                                  Model model){
        //获取到支付宝给我们传来的所有请求数据：
        //    request。 验证签名
        //查出当前登录的用户的所有订单列表数据
        Map<String,Object>  page=new HashMap<>();
        page.put("page",pageNum.toString());
        R r = orderFeignService.listWithItem(page);
        model.addAttribute("orders",r);
        return "orderList";
    }
}
