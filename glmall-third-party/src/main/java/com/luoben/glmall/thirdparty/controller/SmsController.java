package com.luoben.glmall.thirdparty.controller;

import com.luoben.common.utils.R;
import com.luoben.glmall.thirdparty.component.SMSComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsController {

    @Autowired
    private SMSComponent smsComponent;

    /**
     * 提供给别的服务调用
     * @param phone
     * @param code
     * @return
     */
    @GetMapping("/sendcode")
    public R sendSMSCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        smsComponent.sendSMSCode(phone, code);
        return R.ok();
    }
}
