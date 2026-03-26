package com.xcommerce.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(OrderServiceTestConfig.class)
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
