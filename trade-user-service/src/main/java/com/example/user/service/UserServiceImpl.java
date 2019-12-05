package com.example.user.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.api.service.UserService;
import com.example.pojo.pojo.TradeUser;
import com.example.user.mapper.TradeUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author LinYongJin
 * @date 2019/12/5 21:20
 */
@Component
@Service(interfaceClass = UserService.class)
public class UserServiceImpl implements UserService{

    @Autowired
    private TradeUserMapper tradeUserMapper;

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
}
