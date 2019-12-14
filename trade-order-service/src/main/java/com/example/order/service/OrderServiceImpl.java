package com.example.order.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.example.api.service.CouponService;
import com.example.api.service.GoodsService;
import com.example.api.service.OrderService;
import com.example.api.service.UserService;
import com.example.common.constant.ShopCode;
import com.example.common.exception.CastException;
import com.example.common.utils.IDWorker;
import com.example.order.mapper.TradeOrderMapper;
import com.example.pojo.entry.MQEntity;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.*;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:11
 */
@Component
@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Reference
    private GoodsService goodsService;

    @Reference
    private UserService userService;

    @Reference
    private CouponService couponService;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private IDWorker idWorker;

    @Value("${mq.order.topic}")
    private String topic;

    @Value("${mq.order.tag.cancel}")
    private String tag;

    @Override
    public Result confirmOrder(TradeOrder order) {
        //1.校验订单
        checkOrder(order);
        //2.生成预订单
        savePreOrder(order);
        try {
            //3.扣减库存
            reduceGoodsNum(order);
            //4.扣减优惠券
            changeCouponStatus(order);
            //5.使用余额
            reduceUserMoney(order);
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
            //6.确认订单
            updateOrderStatus(order);
            //7.返回成功状态
            logger.info("订单:["+order.getOrderId()+"]确认成功");
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            //1.确认订单失败,发送消息
            MQEntity entity = new MQEntity();
            entity.setCouponId(order.getCouponId());
            entity.setGoodsId(order.getGoodsId());
            entity.setGoodsNum(order.getGoodsNumber());
            entity.setUserId(order.getUserId());
            entity.setUserMoney(order.getMoneyPaid());
            //2.返回失败状态
            Message message = new Message(topic, tag, order.getOrderId().toString(), JSON.toJSONString(entity).getBytes(StandardCharsets.UTF_8));
            try {
                rocketMQTemplate.getProducer().send(message);
            } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException ex) {
                ex.printStackTrace();
                CastException.cast(ShopCode.SHOP_MQ_SEND_MESSAGE_FAIL);
            }
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
    }


    private void checkOrder(TradeOrder order) {
        //1.校验订单是否存在
        if (order == null) {
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        //2.校验订单中的商品是否存在
        TradeGoods good = goodsService.findOne(order.getGoodsId());
        if (good == null) {
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
        }
        //3.校验下单用户是否存在
        TradeUser tradeUser = userService.findOne(order.getUserId());
        if (tradeUser == null) {
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }
        //4.校验商品单价是否合法
        if (order.getGoodsPrice().compareTo(good.getGoodsPrice()) != 0) {
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }
        //5.校验订单商品数量是否合法
        if (order.getGoodsNumber() > good.getGoodsNumber()) {
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        logger.info("校验订单通过");
    }

    private long savePreOrder(TradeOrder order) {
        //1.设置订单状态为不可见
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());
        //2.订单ID
        order.setOrderId(idWorker.nextId());
        //3.核算运费是否正确
        BigDecimal shippingFee = calculateShippingFee(order.getOrderAmount());
        if (order.getShippingFee().compareTo(shippingFee) != 0) {
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }
        //4.查询订单总价是否正确
        BigDecimal orderAmount = order.getGoodsPrice().multiply(new BigDecimal(order.getGoodsNumber()));
        if (orderAmount.compareTo(order.getOrderAmount()) != 0) {
            CastException.cast(ShopCode.SHOP_ORDERAMOUNT_INVALID);
        }
        //5.判断优惠券是否合法
        Long couponId = order.getCouponId();
        if (couponId != null) {
            TradeCoupon coupon = couponService.findOne(couponId);
            //优惠券不存在
            if (coupon == null) {
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            //优惠券已使用
            if (coupon.getIsUsed().toString().equals(ShopCode.SHOP_COUPON_ISUSED.getCode().toString())) {
                CastException.cast(ShopCode.SHOP_COUPON_INVALIED);
            }
            //设置优惠券的金额
            order.setCouponPaid(coupon.getCouponPrice());
        } else {
            order.setCouponPaid(BigDecimal.ZERO);
        }
        //6.判断余额是否正确
        BigDecimal moneyPaid = order.getMoneyPaid();
        if (moneyPaid != null) {
            //比较余额是否大于0
            int compare = moneyPaid.compareTo(BigDecimal.ZERO);
            if (compare == -1) {
                CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
            }
            if (compare == 1) {
                TradeUser tradeUser = userService.findOne(order.getUserId());
                if (tradeUser == null) {
                    CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
                }
                if (moneyPaid.compareTo(new BigDecimal(tradeUser.getUserMobile())) == 1) {
                    CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALID);
                }
            }
        } else {
            order.setMoneyPaid(BigDecimal.ZERO);
        }
        //7.计算订单支付总价
        order.setPayAmount(orderAmount.subtract(order.getCouponPaid()).subtract(order.getMoneyPaid()));
        //8.保存订单
        int index = tradeOrderMapper.insertSelective(order);
        if (index != ShopCode.SHOP_SUCCESS.getCode()) {
            CastException.cast(ShopCode.SHOP_ORDER_SAVE_ERROR);
        }
        logger.info("订单[" + order.getOrderId() + "]生成预订单成功");
        return order.getOrderId();
    }

    private BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        if (orderAmount.compareTo(new BigDecimal(100)) > 0) {
            return BigDecimal.ZERO;
        } else {
            return new BigDecimal(100);
        }
    }

    private void reduceGoodsNum(TradeOrder order) {
        TradeGoodsNumberLog log = new TradeGoodsNumberLog();
        log.setGoodsNumber(order.getGoodsNumber());
        log.setGoodsId(order.getGoodsId());
        log.setOrderId(order.getOrderId());
        log.setGoodsNumber(order.getGoodsNumber());
        Result result = goodsService.reduceGoodsNum(log);
        if (result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())) {
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }
        logger.info("订单:["+order.getOrderId()+"]扣减库存["+order.getGoodsNumber()+"个]成功");
    }

    private void changeCouponStatus(TradeOrder order) {
        //判断用户是否使用优惠券
        if (order.getCouponId() != null) {
            TradeCoupon coupon = new TradeCoupon();
            coupon.setCouponId(order.getCouponId());
            coupon.setCouponPrice(order.getCouponPaid());
            coupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());
            coupon.setUsedTime(new Date());
            Result result = couponService.changeCouponStatus(coupon);
            if (result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())) {
                CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
            }
            logger.info("订单:["+order.getOrderId()+"]使用扣减优惠券["+coupon.getCouponPrice()+"元]成功");
        }
    }

    private void reduceUserMoney(TradeOrder order) {
        //判断订单的余额信息是否合法
        if (order.getMoneyPaid() != null && order.getMoneyPaid().compareTo(BigDecimal.ZERO) == 1) {
            TradeUserMoneyLog log = new TradeUserMoneyLog();
            log.setOrderId(order.getOrderId());
            log.setUserId(order.getUserId());
            log.setUseMoney(order.getMoneyPaid());
            log.setMoneyLogType(ShopCode.SHOP_USER_MONEY_PAID.getCode());
            //扣减余额
            Result result = userService.changeUserMoney(log);
            if (result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())) {
                CastException.cast(ShopCode.SHOP_USER_MONEY_REDUCE_FAIL);
            }
            logger.info("订单:["+order.getOrderId()+"扣减余额["+order.getMoneyPaid()+"元]成功]");
        }
    }

    private void updateOrderStatus(TradeOrder order) {
        order.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        order.setConfirmTime(new Date());
        int index = tradeOrderMapper.updateByPrimaryKeySelective(order);
        if (index < 0) {
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
        }
        logger.info("订单:["+order.getOrderId()+"]状态修改成功");
    }
}
