package com.example.api.service;

import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeOrder;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:08
 */
public interface OrderService {

    /**
     * @param order
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/5 21:10
     * @description 下订单
     */
    Result confirmOrder(TradeOrder order);
}
