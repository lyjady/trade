package com.example.order.listener;

import com.alibaba.fastjson.JSON;
import com.example.common.constant.ShopCode;
import com.example.order.mapper.TradeOrderMapper;
import com.example.pojo.entry.MQEntity;
import com.example.pojo.pojo.TradeOrder;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * @author LinYongJin
 * @date 2019/12/14 15:43
 */
@Component
@RocketMQMessageListener(consumerGroup = "${mq.order.consumer.group.name}", topic = "orderTopic", messageModel = MessageModel.BROADCASTING)
public class OrderListener implements RocketMQListener<MessageExt> {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        log.info("接收消息成功");
        try {
            String jsonBody = new String(messageExt.getBody(), StandardCharsets.UTF_8);
            MQEntity entity = JSON.parseObject(jsonBody, MQEntity.class);
            TradeOrder order = tradeOrderMapper.selectByPrimaryKey(entity.getOrderId());
            order.setOrderStatus(ShopCode.SHOP_ORDER_CANCEL.getCode());
            tradeOrderMapper.updateByPrimaryKeySelective(order);
            log.info("订单: " + entity.getOrderId() + "订单已取消");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("处理消息失败");
        }
    }
}
