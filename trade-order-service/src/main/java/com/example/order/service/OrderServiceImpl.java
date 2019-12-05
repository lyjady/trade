package com.example.order.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.service.CouponService;
import com.example.api.service.GoodsService;
import com.example.api.service.OrderService;
import com.example.api.service.UserService;
import com.example.common.constant.ShopCode;
import com.example.common.exception.CastException;
import com.example.common.utils.IDWorker;
import com.example.order.mapper.TradeOrderMapper;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private IDWorker idWorker;

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

            //6.确认订单

            //7.返回成功状态

        } catch (Exception e) {
            //1.确认订单失败,发送消息

            //2.返回失败状态
        }
        return null;
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
            order.setCouponPaid(BigDecimal.ZERO);
        }
        //7.计算订单支付总价
        order.setPayAmount(orderAmount.subtract(moneyPaid).subtract(order.getCouponPaid()));
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
        log.setGoodsNumber(order.getGoodsNumber());
    }

    private void changeCouponStatus(TradeOrder order) {
        //判断用户是否使用优惠券
        if (order.getCouponId() != null) {
            TradeCoupon coupon = new TradeCoupon();
            coupon.setCouponId(order.getCouponId());
            coupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());
            coupon.setUsedTime(new Date());
            Result result = couponService.changeCouponStatus(coupon);
            if (!result.getSuccess()) {
                CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
            }
            logger.info("订单:["+order.getOrderId()+"]使用扣减优惠券["+coupon.getCouponPrice()+"元]成功");
        }
    }
}
