package com.example.goods.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.service.GoodsService;
import com.example.common.constant.ShopCode;
import com.example.common.exception.CastException;
import com.example.goods.mapper.TradeGoodsMapper;
import com.example.goods.mapper.TradeGoodsNumberLogMapper;
import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeGoods;
import com.example.pojo.pojo.TradeGoodsNumberLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:16
 */
@Component
@Service(interfaceClass = GoodsService.class)
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private TradeGoodsMapper tradeGoodsMapper;

    @Autowired
    private TradeGoodsNumberLogMapper tradeGoodsNumberLogMapper;

    /**
     * @param id
     * @return com.example.pojo.pojo.TradeGoods
     * @author LinYongJin
     * @date 2019/12/5 21:14
     * @description 根据id查询商品
     */
    @Override
    public TradeGoods findOne(Long id) {
        return tradeGoodsMapper.selectByPrimaryKey(id);
    }

    /**
     * @param numberLog
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/5 21:56
     * @description 扣减库存
     */
    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog numberLog) {
        if (numberLog == null) {
            System.out.println("GoodsServiceImpl.reduceGoodsNum numberLog == null");;
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //查询出商品
        TradeGoods tradeGoods = tradeGoodsMapper.selectByPrimaryKey(numberLog.getGoodsId());
        if (tradeGoods.getGoodsNumber() < numberLog.getGoodsNumber()) {
            System.out.println("GoodsServiceImpl.reduceGoodsNum tradeGoods.getGoodsNumber() < numberLog.getGoodsNumber()");
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        //减库存
        tradeGoods.setGoodsNumber(tradeGoods.getGoodsNumber() - numberLog.getGoodsNumber());
        tradeGoodsMapper.updateByPrimaryKeySelective(tradeGoods);

        numberLog.setLogTime(new Date());
        numberLog.setGoodsNumber(-(numberLog.getGoodsNumber()));
        tradeGoodsNumberLogMapper.insertSelective(numberLog);

        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
