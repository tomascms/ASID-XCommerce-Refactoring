package com.xcommerce.order_service;

import com.xcommerce.order_service.client.CartClient;
import com.xcommerce.order_service.client.CatalogClient;
import com.xcommerce.order_service.client.InventoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OrderServiceApplicationTests {

	@MockBean
	private CartClient cartClient;

	@MockBean
	private CatalogClient catalogClient;

	@MockBean
	private InventoryClient inventoryClient;

	@Test
	void contextLoads() {
	}

}
