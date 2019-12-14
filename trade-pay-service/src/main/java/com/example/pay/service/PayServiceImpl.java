package com.example.pay.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.service.PayService;
import com.example.common.constant.ShopCode;
import com.example.common.exception.CastException;
import com.example.common.utils.IDWorker;
import com.example.pay.mapper.TradePayMapper;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradePay;
import com.example.pojo.pojo.TradePayExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author LinYongJin
 * @date 2019/12/14 17:44
 */
@Component
@Service(interfaceClass = PayService.class)
public class PayServiceImpl implements PayService {

    @Autowired
    private TradePayMapper tradePayMapper;

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
}
