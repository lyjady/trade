package com.example.api.service;

import com.example.pojo.entry.Result;
import com.example.pojo.pojo.TradeGoods;
import com.example.pojo.pojo.TradeGoodsNumberLog;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:14
 */
public interface GoodsService {

    /**
     * @param id
     * @return com.example.pojo.pojo.TradeGoods
     * @author LinYongJin
     * @date 2019/12/5 21:14
     * @description 根据id查询商品
     */
    TradeGoods findOne(Long id);

    /**
     * @param numberLog
     * @return com.example.pojo.entry.Result
     * @author LinYongJin
     * @date 2019/12/5 21:56
     * @description 扣减库存
     */
    Result reduceGoodsNum(TradeGoodsNumberLog numberLog);
}
