package com.example.goods.mapper;

import com.example.pojo.pojo.TradeGoodsNumberLog;
import com.example.pojo.pojo.TradeGoodsNumberLogExample;
import com.example.pojo.pojo.TradeGoodsNumberLogKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TradeGoodsNumberLogMapper {
    int countByExample(TradeGoodsNumberLogExample example);

    int deleteByExample(TradeGoodsNumberLogExample example);

    int deleteByPrimaryKey(TradeGoodsNumberLogKey key);

    int insert(TradeGoodsNumberLog record);

    int insertSelective(TradeGoodsNumberLog record);

    List<TradeGoodsNumberLog> selectByExample(TradeGoodsNumberLogExample example);

    TradeGoodsNumberLog selectByPrimaryKey(TradeGoodsNumberLogKey key);

    int updateByExampleSelective(@Param("record") TradeGoodsNumberLog record, @Param("example") TradeGoodsNumberLogExample example);

    int updateByExample(@Param("record") TradeGoodsNumberLog record, @Param("example") TradeGoodsNumberLogExample example);

    int updateByPrimaryKeySelective(TradeGoodsNumberLog record);

    int updateByPrimaryKey(TradeGoodsNumberLog record);
}