package com.example.coupon.listener;

import com.alibaba.fastjson.JSON;
import com.example.common.constant.ShopCode;
import com.example.coupon.mapper.TradeCouponMapper;
import com.example.pojo.entry.MQEntity;
import com.example.pojo.pojo.TradeCoupon;
import org.apache.rocketmq.common.message.Message;
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
 * @date 2019/12/14 16:54
 */
@Component
@RocketMQMessageListener(consumerGroup = "${mq.order.consumer.group.name}", topic = "orderTopic", messageModel = MessageModel.BROADCASTING)
public class CouponListener implements RocketMQListener<MessageExt> {

    private static final Logger log = LoggerFactory.getLogger(CouponListener.class);

    @Autowired
    private TradeCouponMapper tradeCouponMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        try {
            MQEntity entity = JSON.parseObject(body, MQEntity.class);
            //查询优惠券信息
            TradeCoupon coupon = tradeCouponMapper.selectByPrimaryKey(entity.getCouponId());
            if (coupon != null) {
                coupon.setUsedTime(null);
                coupon.setIsUsed(ShopCode.SHOP_COUPON_UNUSED.getCode());
                tradeCouponMapper.updateByPrimaryKey(coupon);
            }
            log.info("优惠券回退成功");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("优惠券回退失败");
        }
    }
}
