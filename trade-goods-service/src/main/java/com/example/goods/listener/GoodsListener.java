package com.example.goods.listener;

import com.alibaba.fastjson.JSON;
import com.example.common.constant.ShopCode;
import com.example.goods.mapper.TradeGoodsMapper;
import com.example.goods.mapper.TradeGoodsNumberLogMapper;
import com.example.goods.mapper.TradeMqConsumerLogMapper;
import com.example.pojo.entry.MQEntity;
import com.example.pojo.pojo.*;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author LinYongJin
 * @date 2019/12/14 15:59
 */
@Component
@RocketMQMessageListener(consumerGroup = "${mq.order.consumer.group.name}", topic = "orderTopic", messageModel = MessageModel.BROADCASTING)
public class GoodsListener implements RocketMQListener<MessageExt> {


    private static final Logger log = LoggerFactory.getLogger(GoodsListener.class);

    @Autowired
    private TradeGoodsMapper tradeGoodsMapper;

    @Autowired
    private TradeMqConsumerLogMapper tradeMqConsumerLogMapper;

    @Autowired
    private TradeGoodsNumberLogMapper tradeGoodsNumberLogMapper;

    @Value("${mq.order.consumer.group.name}")
    private String consumerGroup;

    @Override
    public void onMessage(MessageExt messageExt) {
        log.info("收到消息开始消费");
        String messageId = null;
        String tags = null;
        String keys = null;
        String body = null;
        try {
            body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
            tags = messageExt.getTags();
            keys = messageExt.getKeys();
            messageId = messageExt.getMsgId();
            MQEntity entity = JSON.parseObject(body, MQEntity.class);
            //查询改是否有该消息的消费记录
            TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
            primaryKey.setGroupName(consumerGroup);
            primaryKey.setMsgKey(keys);
            primaryKey.setMsgTag(tags);
            TradeMqConsumerLog consumerLog = tradeMqConsumerLogMapper.selectByPrimaryKey(primaryKey);
            if (consumerLog != null) {
                //该消息已经消费过了
                //获取消费状态
                Integer statusCode = consumerLog.getConsumerStatus();
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode().intValue() == statusCode.intValue()) {
                    //说明消息已经处理成功了,不能重复消费
                    log.info("该消息已经消费成功了");
                    return;
                }
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode() == statusCode.intValue()) {
                    //说明该消息正在处理
                    log.info("该消息正在处理中");
                    return;
                }
                //获得处理失败次数
                Integer failTimes = consumerLog.getConsumerTimes();
                if (failTimes > 3) {
                    //说明到达了失败次数的上限
                    log.info("该消息已经达到了处理失败的上限");
                    return;
                }
                //设置消息的状态是处理中
                consumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
                //使用数据库乐观锁更新
                TradeMqConsumerLogExample example = new TradeMqConsumerLogExample();
                TradeMqConsumerLogExample.Criteria criteria = example.createCriteria();
                criteria.andMsgTagEqualTo(consumerLog.getMsgTag());
                criteria.andMsgKeyEqualTo(consumerLog.getMsgKey());
                criteria.andGroupNameEqualTo(consumerGroup);
                criteria.andConsumerTimesEqualTo(consumerLog.getConsumerTimes());
                int r = tradeMqConsumerLogMapper.updateByExampleSelective(consumerLog, example);
                if (r < 0) {
                    log.info("并发修改,为处理成功");
                }
            } else {
                //如果这条消息没有消费过
                TradeMqConsumerLog tradeMqConsumerLog = new TradeMqConsumerLog();
                tradeMqConsumerLog.setMsgKey(keys);
                tradeMqConsumerLog.setMsgId(messageId);
                tradeMqConsumerLog.setMsgTag(tags);
                tradeMqConsumerLog.setMsgBody(body);
                tradeMqConsumerLog.setGroupName(consumerGroup);
                tradeMqConsumerLog.setConsumerTimes(0);
                tradeMqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
                tradeMqConsumerLogMapper.insertSelective(tradeMqConsumerLog);
            }
            //回退库存
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            Long goodsId = mqEntity.getGoodsId();
            TradeGoods goods = tradeGoodsMapper.selectByPrimaryKey(goodsId);
            goods.setGoodsNumber(goods.getGoodsNumber() + mqEntity.getGoodsNum());
            tradeGoodsMapper.updateByPrimaryKeySelective(goods);

            //纪律库存操作日志
            TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
            goodsNumberLog.setOrderId(mqEntity.getOrderId());
            goodsNumberLog.setGoodsId(goodsId);
            goodsNumberLog.setGoodsNumber(mqEntity.getGoodsNum());
            goodsNumberLog.setLogTime(new Date());
            tradeGoodsNumberLogMapper.insertSelective(goodsNumberLog);

            //将消息状态设置为成功
            TradeMqConsumerLog newConsumerLog = new TradeMqConsumerLog();
            newConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
            newConsumerLog.setConsumerTimestamp(new Date());
            newConsumerLog.setGroupName(consumerGroup);
            newConsumerLog.setMsgKey(keys);
            newConsumerLog.setMsgTag(tags);
            tradeMqConsumerLogMapper.updateByPrimaryKey(newConsumerLog);
            log.info("回退库存成功");
        } catch (Exception e) {
            e.printStackTrace();
            //发生异常需要将失败次数+1,如果查询出来如果为null则是第一次失败那么只需设置为1
            TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
            primaryKey.setMsgTag(tags);
            primaryKey.setMsgKey(keys);
            primaryKey.setGroupName(consumerGroup);
            TradeMqConsumerLog mqConsumerLog = tradeMqConsumerLogMapper.selectByPrimaryKey(primaryKey);
            if (mqConsumerLog == null) {
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode());
                mqConsumerLog.setMsgBody(body);
                mqConsumerLog.setMsgId(messageId);
                mqConsumerLog.setConsumerTimes(1);
                tradeMqConsumerLogMapper.insertSelective(mqConsumerLog);
            } else {
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes() + 1);
                tradeMqConsumerLogMapper.updateByPrimaryKeySelective(mqConsumerLog);
            }
        }
    }
}
