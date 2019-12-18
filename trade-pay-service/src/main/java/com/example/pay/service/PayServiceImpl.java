package com.example.pay.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.example.api.service.PayService;
import com.example.common.constant.ShopCode;
import com.example.common.exception.CastException;
import com.example.common.utils.IDWorker;
import com.example.pay.mapper.TradeMqProducerTempMapper;
import com.example.pay.mapper.TradePayMapper;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeMqProducerTemp;
import com.example.pojo.pojo.TradePay;
import com.example.pojo.pojo.TradePayExample;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author LinYongJin
 * @date 2019/12/14 17:44
 */
@Component
@Service(interfaceClass = PayService.class)
public class PayServiceImpl implements PayService {

    @Autowired
    private TradePayMapper tradePayMapper;

    @Autowired
    private TradeMqProducerTempMapper tradeMqProducerTempMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${mq.order.topic}")
    private String topic;

    @Value("${mq.pay.tag}")
    private String tags;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    private static final Logger log = LoggerFactory.getLogger(PayServiceImpl.class);

    /**
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/14 17:44
     * @description 创建支付订单
     */
    @Override
    public Result createPayment(TradePay tradePay) {
        try {
            //判断订单支付状态
            TradePayExample example = new TradePayExample();
            TradePayExample.Criteria criteria = example.createCriteria();
            criteria.andOrderIdEqualTo(tradePay.getOrderId());
            criteria.andIsPaidEqualTo(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
            int count = tradePayMapper.countByExample(example);
            if (count > 0) {
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
            }
            //设置订单支付状态为未支付
            tradePay.setPayId(new IDWorker(1, 2).nextId());
            tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
            //保存订单信息
            tradePayMapper.insertSelective(tradePay);
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
    }

    /**
     * @param tradePay
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/18 20:25
     * @description 支付回调
     */
    @Override
    public Result callbackPay(TradePay tradePay) {
        //1.判断用户支付状态
        if (tradePay.getIsPaid().equals(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode())) {
            tradePay = tradePayMapper.selectByPrimaryKey(tradePay.getPayId());
            if (tradePay == null) {
                CastException.cast(ShopCode.SHOP_PAYMENT_NOT_FOUND);
            }
            //2.更新支付状态
            tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
            int index = tradePayMapper.updateByPrimaryKeySelective(tradePay);
            if (index == 1) {
                //3.创建支付成功的消息
                TradeMqProducerTemp temp = new TradeMqProducerTemp();
                temp.setCreateTime(new Date());
                temp.setGroupName(producerGroup);
                temp.setMsgTag(tags);
                temp.setMsgKey(String.valueOf(tradePay.getPayId()));
                temp.setMsgTopic("orderTopic");
                temp.setMsgBody(JSON.toJSONString(tradePay));
                temp.setId(String.valueOf(new IDWorker(1, 2)));
                //4.将消息持久化到数据库
                tradeMqProducerTempMapper.insertSelective(temp);
                //5.发送消息
                Message message = new Message("orderTopic", tags, String.valueOf(tradePay.getPayId()), JSON.toJSONString(tradePay).getBytes(StandardCharsets.UTF_8));
                SendResult sendResult = sendMessage(message);
                if (sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
                    //6.等待发送结果,如果MQ收到消息则删除数据库中的记录
                    log.info("支付成功消息发送成功");
                    tradeMqProducerTempMapper.deleteByPrimaryKey(String.valueOf(tradePay.getPayId()));
                }
            }
        } else {
            CastException.cast(ShopCode.SHOP_PAYMENT_PAY_ERROR);
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

    private SendResult sendMessage(Message message) {
        try {
            log.info("发送支付成功消息");
            return rocketMQTemplate.getProducer().send(message);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            e.printStackTrace();
            return new SendResult();
        }
    }
}
