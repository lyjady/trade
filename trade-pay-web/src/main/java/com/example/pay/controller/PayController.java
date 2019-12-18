package com.example.pay.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.example.api.service.PayService;
import com.example.pojo.pojo.TradePay;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LinYongJin
 * @date 2019/12/18 21:33
 */
@RequestMapping("/pay")
@RestController
public class PayController {

    @Reference
    private PayService payService;

    @RequestMapping("/createPayOrder")
    public void createPayOrder(@RequestBody TradePay tradePay) {
        payService.createPayment(tradePay);
    }

    @RequestMapping("/callback")
    public void callback(@RequestBody TradePay tradePay) {
        payService.callbackPay(tradePay);
    }
}
