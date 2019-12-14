package com.example.user.listener;

import com.alibaba.fastjson.JSON;
import com.example.api.service.UserService;
import com.example.common.constant.ShopCode;
import com.example.pojo.entry.MQEntity;
import com.example.pojo.pojo.TradeUserMoneyLog;
import com.example.user.mapper.TradeUserMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * @author LinYongJin
 * @date 2019/12/14 17:08
 */
@Component
@RocketMQMessageListener(consumerGroup = "${mq.order.consumer.group.name}", topic = "orderTopic", messageModel = MessageModel.BROADCASTING)
public class UserListener implements RocketMQListener<MessageExt> {

    private static final Logger log = LoggerFactory.getLogger(UserListener.class);

    @Autowired
    private UserService userService;

    @Override
    public void onMessage(MessageExt messageExt) {
        //回退余额需要考虑到幂等性问题,这里简单编写
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("开始消费数据");
        try {
            MQEntity entity = JSON.parseObject(body, MQEntity.class);
            TradeUserMoneyLog moneyLog = new TradeUserMoneyLog();
            if (moneyLog.getUseMoney() != null && moneyLog.getUseMoney().compareTo(BigDecimal.ZERO) > 0) {
                moneyLog.setUseMoney(entity.getUserMoney());
                moneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
                moneyLog.setUserId(entity.getUserId());
                moneyLog.setOrderId(entity.getOrderId());
                userService.changeUserMoney(moneyLog);
                log.info("回退金额成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("回退失败");
        }
    }
}
