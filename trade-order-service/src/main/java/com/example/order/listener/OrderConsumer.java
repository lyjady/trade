package com.example.order.listener;

import com.alibaba.fastjson.JSON;
import com.example.common.constant.ShopCode;
import com.example.order.mapper.TradeOrderMapper;
import com.example.pojo.pojo.TradeOrder;
import com.example.pojo.pojo.TradePay;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import java.nio.charset.StandardCharsets;

/**
 * @author LinYongJin
 * @date 2019/12/18 20:53
 */
@Component
@RocketMQMessageListener(topic = "orderTopic", consumerGroup = "orderConsumer", messageModel = MessageModel.BROADCASTING)
public class OrderConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    @Override
    public void onMessage(MessageExt messageExt) {
        TradePay tradePay = JSON.parseObject(new String(messageExt.getBody(), StandardCharsets.UTF_8), TradePay.class);
        TradeOrder order = tradeOrderMapper.selectByPrimaryKey(tradePay.getOrderId());
        order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
        int index = tradeOrderMapper.updateByPrimaryKeySelective(order);
        if (index == 1) {
            log.info("更改订单支付状态为已支付");
        }
    }
}
