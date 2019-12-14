package com.example.api.service;

import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradePay;

/**
 * @author LinYongJin
 * @date 2019/12/14 17:43
 */

public interface PayService {

    /**
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/14 17:44
     * @description 创建支付订单
     */
    Result createPayment(TradePay tradePay);
}
