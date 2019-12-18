package com.example.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.example.api.service.OrderService;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeOrder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LinYongJin
 * @date 2019/12/18 21:28
 */
@RequestMapping("/order")
@RestController
public class OrderController {

    @Reference
    private OrderService orderService;

    @RequestMapping("/confirmOrder")
    public Result confirmOrder(@RequestBody TradeOrder tradeOrder) {
        return orderService.confirmOrder(tradeOrder);
    }
}
