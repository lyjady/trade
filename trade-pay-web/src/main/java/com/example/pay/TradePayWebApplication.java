package com.example.pay;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubboConfiguration
public class TradePayWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradePayWebApplication.class, args);
	}

}
