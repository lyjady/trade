package com.example.order;

import com.example.api.service.OrderService;
import com.example.pojo.pojo.TradeOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
class TradeOrderServiceApplicationTests {

	@Autowired
	private OrderService orderService;

	@Test
	public void takeOrder() {
		long couponId = 345959444133456245L;
		long goodsId = 345959443973935104L;
		long userId = 345963634385633280L;

		TradeOrder order = new TradeOrder();
		order.setCouponId(couponId);
		order.setUserId(userId);
		order.setGoodsId(goodsId);
		order.setAddress("福州");
		order.setGoodsNumber(1);
		order.setOrderAmount(new BigDecimal(8000));
		order.setGoodsPrice(new BigDecimal(8000));
		order.setShippingFee(BigDecimal.ZERO);
		order.setMoneyPaid(new BigDecimal(1000));
		orderService.confirmOrder(order);
	}
}
