package com.luoben.glmall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.luoben.glmall.order.vo.PayVo;
import lombok.Data;
import org.springframework.stereotype.Component;

//@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2016080600181162";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCf/L3NFlxNgDy900oqWU1hiti64GYvnk/cj6URewBxlZICfZfn7E9Fv7e+EmDlzdGsSQCr8P6SP/mYwuJ6zD4XgE2fO8mzc24K557WEBBTRLqAJG8VFOomAmp7233OkOtM8rn0S9XsNVBw86XaI3B9ZYCutsYddyy2z7jtzR+RH49UtXbU4EZs2jEU2t4XA+YOynnjQ/huBYsi6cAiIizpLdv0ul3IumtzisabZAP0JIbf/IkUwe1wQlC79WFz2uxrLDsNifcbdoqVjBNvJgG1l8JjgQ6egeABSATZGlCQVFMkPlUq9bINi0+uB85R9uP9NbVvO7xhznItlUqI5fQ9AgMBAAECggEACmlfTL58yUpkKZJbjD/9ijCIBDlgTQ8HcXojquyPTdUHCIR141D8o7RjV8pIZgwr7gNeONJZLtlc+/UK2iT6kXr3EjrI2JXVfn4uVw36kgCyBm7Yj82po0ma9m6FVHEX7w0Izv40cMfTcVZZ38VYp8B4TGZ2pCpCTa9dAJTNzhreWFlv/XmewOfNdcupsroac4p7mokNjLUjiA5LdjB2mwHhMiH2oCwTAghpnX5lC/xQxaw9ovJPcNskMNrd+BbDF7nKsvDfL2j/y2zVZVKsXORXIDCV0zzPakD7fIfzjMTb/LslZpLIDvrjxHhCNOYYUGlKFXvH2lluBiGNCT+03QKBgQDMnfyhLGVo3gY5Gsb7RhhEEoOxqDREnqEJ9Jz6hgmhso55eIFfWPO/Wiwsawbe12O6IKqX1WqXwfuYCmp2IMjuRpdFdodoJQxN0KQTbz8MaAY253sP9IUK9N7xCpE3qJa9y5g5bu6LFh5CPwhQqX1w9U3wFTCGYaf7/8S+8hThgwKBgQDIKa4R7UpK8vlIAyJDEf9uBsjYIsW/ls8m5Nd0fTA0kj/QsANoT1JGpy4BIn9yKNsqJP/Ab+c/Zu724McSwzXQ2x6/Z54C+JCUf3s/jhf0yfR2YmknEge3ohXHyUmHEKbovlIRTqd/Nx0tKhbrXxuPVOEUHbp1jr+9Nosl6+KnPwKBgQDHlY7W00wzZuWhC8ptTGjc9UY5ov0gta4U3OHFx7pbW6R3PaDLlSNkUZtm1BqGgIfJBJYBtezcDB3Rps9DXCVBrd9dpQjc/84plMqGHmvcORdetJmn7XVcQ4+2g/0z0iD/Djj0RI0vY6quKXd93mT6KnwUI0cKvPSy7D9HlS/i6wKBgBo1QuJ8BF894x/nsSPBBoXcg42xN903HNaF3iQVhCtN/ucPNNMCl85Cc4aYgsFq0g95mrcSr9+gVaejlM1DHBfFqQf8xDa1XxDihDu1GjPmAYlCp2wDM1l68okfNO5nYsNUsCAuAUZp6/sO6MNWx6ADyBi/L7vWvgpDSwLjGOE3AoGAFviiGrLPBmrJK/0FtT5+Ib1JPE2Snn+WXR5AQBLOUkrLvs+CwBKiXZB5pYkKGZfhSBdwurLSaVkVW4zp8TzP6Z/mCG+snK8GT1KVcpGiJzmxhmpexvs4Ee01J6jyAzyQupLm9LJ94DrU44JnFlsiND0l1cbAkFFliCVR2afVzQo=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwFtTvKpoHno+N2CX74pwTZ/U7sK9A5xUNntyJ9hmSR3iIP7aarbF5B3wkmtYJMYYXTtx4g6UmVMELp2KGksWGMtgKns82ZBn/mMAI1kN7S1ivTnwizbBFoRNwXb3vW02RZkxmGDLs5lnMaoad2aiJhpbz/9hdM+iAiGduGnmOrQt2kyvbIxq20A/Rn/tcP4Rh7FYJA7n7bnptl1i93SLOtMyiMZ9zAxwWGrf0rIv8deZGYlrUzoNMnJmkgUsY+YD1QFajVsrQrLjCe9B5v5mx9Jx8nd2TMNCdpmqj3kKBQuoR386mFNzd+sK4aLg4v+t5SQUWsizaWbzlPufuTdaJQIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url="http://iwv3nerz2z.52http.tech/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url="http://member.glmall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    //订单支付超时之间
    private String timeout="30m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
