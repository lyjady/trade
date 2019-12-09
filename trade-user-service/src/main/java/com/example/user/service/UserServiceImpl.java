package com.example.user.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.service.UserService;
import com.example.common.constant.ShopCode;
import com.example.common.exception.CastException;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeUser;
import com.example.pojo.pojo.TradeUserMoneyLog;
import com.example.pojo.pojo.TradeUserMoneyLogExample;
import com.example.user.mapper.TradeUserMapper;
import com.example.user.mapper.TradeUserMoneyLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:20
 */
@Component
@Service(interfaceClass = UserService.class)
public class UserServiceImpl implements UserService{

    @Autowired
    private TradeUserMapper tradeUserMapper;

    @Autowired
    private TradeUserMoneyLogMapper tradeUserMoneyLogMapper;

    /**
     * @param id
     * @return com.example.pojo.pojo.TradeUser
     * @author LinYongJin
     * @date 2019/12/5 21:19
     * @description 根据id查询用户
     */
    @Override
    public TradeUser findOne(long id) {
        return tradeUserMapper.selectByPrimaryKey(id);
    }

    /**
     * @param log
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/9 20:28
     * @description 改变用户余额
     */
    @Override
    public Result changeUserMoney(TradeUserMoneyLog log) {
        //判断请求参数是否合法
        if (log == null || log.getUserId() == null || log.getOrderId() == null || log.getUseMoney().compareTo(BigDecimal.ZERO) == -1) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //查询该订单是否存在付款记录
        TradeUserMoneyLogExample example = new TradeUserMoneyLogExample();
        example.createCriteria().andUserIdEqualTo(log.getUserId()).andOrderIdEqualTo(log.getOrderId());
        int count = tradeUserMoneyLogMapper.countByExample(example);
        TradeUser user = tradeUserMapper.selectByPrimaryKey(log.getUserId());
        System.out.println(user);
        //扣减余额操作
        if (log.getMoneyLogType().equals(ShopCode.SHOP_USER_MONEY_PAID.getCode())) {
            if (count > 0) {
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
            }
            //扣减余额
            user.setUserMoney(new BigDecimal(user.getUserMoney()).subtract(log.getUseMoney()).longValue());
            tradeUserMapper.updateByPrimaryKeySelective(user);
        }
        //回退余额操作
        if (log.getMoneyLogType().equals(ShopCode.SHOP_USER_MONEY_REFUND.getCode())) {
            if (count < 0) {
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY);
            }
            //防止多次退款
            TradeUserMoneyLogExample refundExample = new TradeUserMoneyLogExample();
            refundExample.createCriteria().andUserIdEqualTo(log.getUserId()).andOrderIdEqualTo(log.getOrderId())
                    .andMoneyLogTypeEqualTo(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
            int refundCount = tradeUserMoneyLogMapper.countByExample(refundExample);
            if (refundCount > 0) {
                CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_ALREADY);
            }
            user.setUserMoney(new BigDecimal(user.getUserMoney()).add(log.getUseMoney()).longValue());
            tradeUserMapper.updateByPrimaryKey(user);
        }
        //插入日志
        log.setCreateTime(new Date());
        tradeUserMoneyLogMapper.insertSelective(log);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
