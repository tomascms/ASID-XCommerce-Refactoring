package com.xcommerce.order_service;

import com.xcommerce.order_service.client.CartClient;
import com.xcommerce.order_service.client.CatalogClient;
import com.xcommerce.order_service.client.InventoryClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
public class OrderServiceTestConfig {

    @Bean
    @Primary
    public CartClient mockCartClient() {
        CartClient mock = Mockito.mock(CartClient.class);
        Mockito.when(mock.getCartItems(Mockito.anyString())).thenReturn(List.of());
        return mock;
    }

    @Bean
    @Primary
    public CatalogClient mockCatalogClient() {
        return Mockito.mock(CatalogClient.class);
    }

    @Bean
    @Primary
    public InventoryClient mockInventoryClient() {
        InventoryClient mock = Mockito.mock(InventoryClient.class);
        Mockito.when(mock.checkStock(Mockito.anyLong(), Mockito.anyInt())).thenReturn(true);
        return mock;
    }
}
