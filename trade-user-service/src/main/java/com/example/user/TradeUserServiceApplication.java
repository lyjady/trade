package com.example.user;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubboConfiguration
public class TradeUserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeUserServiceApplication.class, args);
	}

}
