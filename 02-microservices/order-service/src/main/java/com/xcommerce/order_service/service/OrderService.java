package com.xcommerce.order_service.service;

import com.xcommerce.order_service.dto.CartItemDTO;
import com.xcommerce.order_service.dto.CreateOrderRequest;
import com.xcommerce.order_service.dto.OrderItemDTO;
import com.xcommerce.order_service.model.Order;
import com.xcommerce.order_service.model.OrderItem;
import com.xcommerce.order_service.model.OrderStatus;
import com.xcommerce.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired private OrderRepository repository;
    @Autowired private OrderRemoteService orderRemoteService;
    @Autowired private OrderProducer orderProducer;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public Order createOrder(String username) {
        List<CartItemDTO> cartItems = orderRemoteService.getCartItems(username);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio!");
        }

        List<CreateOrderRequest> requests = cartItems.stream().map(item -> {
            CreateOrderRequest request = new CreateOrderRequest();
            request.setUsername(username);
            request.setProductId(item.getProductId());
            request.setQuantity(item.getQuantity());
            return request;
        }).collect(Collectors.toList());

        // Inicia a Saga: cria a ordem em estado HANDLING e publica evento
        Order pendingOrder = buildPendingOrder(username, requests);
        orderRemoteService.clearCart(username);
        orderProducer.sendOrderCreatedEvent(pendingOrder.getId(), pendingOrder.getUsername(), pendingOrder.getItems());
        return pendingOrder;
    }

    @Transactional
    public Order createDirectOrder(String fallbackUsername, CreateOrderRequest request) {
        String username = (request.getUsername() != null && !request.getUsername().trim().isEmpty()) ? request.getUsername() : fallbackUsername;
        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username obrigatorio.");
        }
        // Inicia a Saga: cria a ordem em estado HANDLING e publica evento
        Order pendingOrder = buildPendingOrder(username, Arrays.asList(request));
        orderProducer.sendOrderCreatedEvent(pendingOrder.getId(), pendingOrder.getUsername(), pendingOrder.getItems());
        return pendingOrder;
    }

    public List<Order> getOrdersByCustomer(String username) {
        return repository.findByUsernameOrderByOrderDateDesc(username);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = repository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(newStatus);
        return repository.save(order);
    }
    
    // Novo método para construir e persistir a ordem em estado HANDLING
    @Transactional
    private Order buildPendingOrder(String username, List<CreateOrderRequest> requests) {
        Order order = new Order();
        order.setUsername(username);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.HANDLING);

        List<OrderItem> items = requests.stream().map(request -> {
            OrderItem item = new OrderItem();
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity());
            BigDecimal productPrice = orderRemoteService.getProductPrice(request.getProductId());
            item.setPrice(productPrice.doubleValue());
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        order.setProductId(requests.get(0).getProductId());
        order.setQuantity(requests.stream().mapToInt(CreateOrderRequest::getQuantity).sum());
        order.setTotalAmount(items.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum());

        // Salva a ordem em estado PENDING
        return repository.save(order);
    }

    @Transactional
    public void handleStockReservationEvent(Long orderId, String status, List<OrderItemDTO> failedItems) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found for stock reservation event: " + orderId));

        if ("STOCK_RESERVED_SUCCESS".equals(status)) {
            order.setStatus(OrderStatus.CONFIRMED);
            repository.save(order);
            // Publicar evento de confirmação de encomenda
            kafkaTemplate.send("order-confirmed-events", String.valueOf(order.getId()), "ORDER_CONFIRMED:" + order.getUsername());
        } else if ("STOCK_RESERVATION_FAILED".equals(status)) {
            order.setStatus(OrderStatus.CANCELLED);
            repository.save(order);
            log.info("📦 [ORDER] Encomenda {} cancelada por falta de stock (compensação local já feita no inventário)", orderId);
        }
    }
}
