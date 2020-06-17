package com.luoben.glmall.product.web;

import com.luoben.glmall.product.entity.CategoryEntity;
import com.luoben.glmall.product.service.CategoryService;
import com.luoben.glmall.product.vo.Catelog2VO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        //查询所有一级分类
        List<CategoryEntity> categoryList=categoryService.getLevelOneCategorys();
        model.addAttribute("categorys",categoryList);
        return "index";
    }

    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2VO>> getCatelogJson(){

        Map<String, List<Catelog2VO>> map = categoryService.getCatelogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }

}
