package com.luoben.glmall.seckill.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.luoben.common.exception.BizCodeEnume;
import com.luoben.common.utils.R;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class SeckillSentinelConfig {

    /**
     * 自定义流控异常
     * @return
     */
    @Bean
    public BlockExceptionHandler blockExceptionHandler(){
        return new BlockExceptionHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
                R error = R.error(BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getCode(), BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getMsg());
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write(JSON.toJSONString(error));
            }
        };
    }
}
