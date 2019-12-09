package com.example.coupon;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubboConfiguration
public class TradeCouponServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeCouponServiceApplication.class, args);
	}

}
