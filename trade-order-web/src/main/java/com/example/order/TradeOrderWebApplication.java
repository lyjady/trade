package com.example.order;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubboConfiguration
public class TradeOrderWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeOrderWebApplication.class, args);
	}

}
