package com.example.goods;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubboConfiguration
public class ShopGoodsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopGoodsServiceApplication.class, args);
	}

}
