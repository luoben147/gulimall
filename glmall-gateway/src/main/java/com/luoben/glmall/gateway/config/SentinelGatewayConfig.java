package com.luoben.glmall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.luoben.common.exception.BizCodeEnume;
import com.luoben.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Configuration
public class SentinelGatewayConfig {
    public SentinelGatewayConfig() {
        GatewayCallbackManager.setBlockHandler(((exchange, t) -> {
            R error = R.error(BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getCode(), BizCodeEnume.TOO_MANY_REQUEST_EXCEPTION.getMsg());
            String jsonString = JSON.toJSONString(error);
            // Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(jsonString), String.class);
            return ServerResponse.ok().body(Mono.just(jsonString), String.class);
        }));
    }
}
