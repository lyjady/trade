package com.example.order.config;

import com.example.common.utils.IDWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author LinYongJin
 * @date 2019/12/9 21:03
 */
@Configuration
public class BeanConfig {

    @Bean
    public IDWorker idWorker() {
        return new IDWorker(1, 1);
    }
}
