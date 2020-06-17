package com.luoben.glmall.cart.controller;

import com.luoben.glmall.cart.service.CartService;
import com.luoben.glmall.cart.vo.Cart;
import com.luoben.glmall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 浏览器有一个cookie: user-key: 标识用户身份，一个月后过期
     * 如果第一次使用京东的购物车，都会给一个临时的用户身份
     * 浏览器以后保持，每次访问都会带上
     *
     * 登录：session有
     * 没登录: 按照cookie里面带来user-key来做
     *
     * 第一次：如果没有临时用户，帮忙创建一个临时用户
     *
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {

        Cart cart= cartService.getCart();

        model.addAttribute("cart",cart);

        return "cartList";
    }

    /**
     * 添加商品到购物车
     *
     * RedirectAttributes   ： 重定向的属性对象
     *      addFlashAttribute(); 将数据放在session中，可以在页面取出，但是只能取一次
     *      addAttribute("skuId",skuId);   将数据放在url后面
     *
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId,num);
        ra.addAttribute("skuId",skuId);
        //重定向到添加商品到购物车成功页面。。 防止刷新重复提交添加请求
        return "redirect:http://cart.glmall.com/addToCartSuccess.html";
    }

    /**
     * 跳转到成功页
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model){
        //再次查询购物车数据
        CartItem cartItem= cartService.getCartItem(skuId);
        model.addAttribute("item",cartItem);
        return "success";
    }

    /**
     * 购物车勾选商品
     * @param skuId
     * @param check
     * @return
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check")Integer check){

        cartService.checkItem(skuId,check);

        //重定向到购物车列表页    刷新
        return "redirect:http://cart.glmall.com/cart.html";
    }

    /**
     * 改变商品数量
     * @return
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num")Integer num){
        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.glmall.com/cart.html";
    }

    /**
     * 删除购物项
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.glmall.com/cart.html";
    }

    /**
     * 获取当前用户的所有选中的购物项
     * @return
     */
    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){

        return cartService.getUserCartItems();
    }

}
