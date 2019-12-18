package com.example.pay;

import com.alibaba.dubbo.config.annotation.Reference;
import com.example.api.service.PayService;
import com.example.common.constant.ShopCode;
import com.example.pojo.pojo.TradePay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;

@SpringBootTest
class TradePayServiceApplicationTests {

	@Autowired
	private PayService payService;

	@Test
	void contextLoads() {
		TradePay tradePay = new TradePay();
		tradePay.setOrderId(401551357435318272L);
		tradePay.setPayAmount(new BigDecimal(6000));

		payService.createPayment(tradePay);
	}

	@Test
	public void testCallback() throws IOException {
		TradePay tradePay = new TradePay();
		tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
		tradePay.setPayId(404783925760761856L);
		tradePay.setOrderId(401551357435318272L);
		payService.callbackPay(tradePay);
		System.in.read();
	}
}
