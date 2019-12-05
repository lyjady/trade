package com.example.coupon.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.service.CouponService;
import com.example.common.constant.ShopCode;
import com.example.common.exception.CastException;
import com.example.coupon.mapper.TradeCouponMapper;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeCoupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:38
 */
@Component
@Service(interfaceClass = CouponService.class)
public class CouponServiceImpl implements CouponService {

    @Autowired
    private TradeCouponMapper tradeCouponMapper;

    /**
     * @param id
     * @return com.example.pojo.pojo.TradeCoupon
     * @author LinYongJin
     * @date 2019/12/5 21:37
     * @description 根据id查询优惠券
     */
    @Override
    public TradeCoupon findOne(long id) {
        return tradeCouponMapper.selectByPrimaryKey(id);
    }

    /**
     * @param coupon
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/5 22:05
     * @description 更新优惠券状态
     */
    @Override
    public Result changeCouponStatus(TradeCoupon coupon) {
        try {
            if (coupon == null || coupon.getCouponId() == null) {
                CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
            }
            tradeCouponMapper.updateByPrimaryKeySelective(coupon);
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
    }
}
