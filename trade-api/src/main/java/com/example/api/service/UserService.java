package com.example.api.service;

import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeUser;
import com.example.pojo.pojo.TradeUserMoneyLog;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:19
 */
public interface UserService {

    /**
     * @param id
     * @return com.example.pojo.pojo.TradeUser
     * @author LinYongJin
     * @date 2019/12/5 21:19
     * @description 根据id查询用户
     */
    TradeUser findOne(long id);

    /**
     * @param log
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/9 20:28
     * @description 改变用户余额
     */
    Result changeUserMoney(TradeUserMoneyLog log);
}
