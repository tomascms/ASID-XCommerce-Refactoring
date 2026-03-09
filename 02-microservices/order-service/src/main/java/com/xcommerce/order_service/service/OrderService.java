package com.xcommerce.order_service.service;

import com.xcommerce.order_service.client.CartClient;
import com.xcommerce.order_service.client.InventoryClient; // Importa o novo cliente
import com.xcommerce.order_service.dto.CartItemDTO;
import com.xcommerce.order_service.model.Order;
import com.xcommerce.order_service.model.OrderItem;
import com.xcommerce.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importante para rollback se algo falhar
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    @Autowired private OrderRepository repository;
    @Autowired private CartClient cartClient;
    @Autowired private InventoryClient inventoryClient; // Injetar o Inventory

    @Transactional // Se o abate de stock falhar, a encomenda não é guardada
    public Order createOrder(String username) {
        // 1. Obter itens do carrinho
        List<CartItemDTO> cartItems = cartClient.getCartItems(username);
        if(cartItems.isEmpty()) throw new RuntimeException("Carrinho vazio!");

        // 2. VALIDAR STOCK de todos os itens antes de fazer qualquer coisa
        for (CartItemDTO item : cartItems) {
            boolean hasStock = inventoryClient.checkStock(item.getProductId(), item.getQuantity());
            if (!hasStock) {
                throw new RuntimeException("Stock insuficiente para o produto ID: " + item.getProductId());
            }
        }

        // 3. Criar a Encomenda
        Order order = new Order();
        order.setUsername(username);
        order.setOrderDate(LocalDateTime.now());
        
        List<OrderItem> items = cartItems.stream().map(c -> {
            OrderItem i = new OrderItem();
            i.setProductId(c.getProductId());
            i.setQuantity(c.getQuantity());
            i.setPrice(25.0); // No futuro: ir buscar ao Catalog Service
            return i;
        }).toList();

        order.setItems(items);
        order.setTotalAmount(items.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum());

        // 4. ABATER STOCK no Inventory Service
        for (CartItemDTO item : cartItems) {
            inventoryClient.decreaseStock(item.getProductId(), item.getQuantity());
        }

        // 5. Guardar encomenda e Limpar carrinho
        Order savedOrder = repository.save(order);
        cartClient.clearCart(username); 
        
        return savedOrder;
    }
}