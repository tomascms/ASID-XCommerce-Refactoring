package com.xcommerce.order_service.dto;

import com.xcommerce.order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StockReservationEventListener {
    private static final Logger log = LoggerFactory.getLogger(StockReservationEventListener.class);

    private final OrderService orderService;

    public StockReservationEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "stock-reservation-events", groupId = "order-group")
    public void handleStockReservationEvent(
            String message,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        try {
            Long orderId = Long.parseLong(key);
            Map<String, String> eventData = Arrays.stream(message.split(","))
                    .map(s -> s.split(":"))
                    .collect(Collectors.toMap(a -> a[0], a -> a[1]));

            String status = eventData.get("status");
            List<OrderItemDTO> failedItems = eventData.containsKey("failedItems") ?
                    Arrays.stream(eventData.get("failedItems").split(";")).map(OrderItemDTO::fromString).collect(Collectors.toList()) :
                    List.of();
            orderService.handleStockReservationEvent(orderId, status, failedItems);
        } catch (Exception e) {
            log.error("❌ [ORDER] Erro ao processar evento de reserva de stock: {}", e.getMessage());
        }
    }
}