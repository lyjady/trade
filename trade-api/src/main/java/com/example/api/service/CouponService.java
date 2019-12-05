package com.example.api.service;

import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeCoupon;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:37
 */
public interface CouponService {

    /**
     * @param id
     * @return com.example.pojo.pojo.TradeCoupon
     * @author LinYongJin
     * @date 2019/12/5 21:37
     * @description 根据id查询优惠券
     */
    TradeCoupon findOne(long id);

    /**
     * @param coupon
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/5 22:05
     * @description 更新优惠券状态
     */
    Result changeCouponStatus(TradeCoupon coupon);
}
