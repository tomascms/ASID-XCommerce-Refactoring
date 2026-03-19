package com.xcommerce.order_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderEvents(String username, Long productId, Integer quantity) {
        this.kafkaTemplate.send("order-placed", username);

        String inventoryMessage = productId + ":" + quantity;
        this.kafkaTemplate.send("order-placed-events", inventoryMessage);

        System.out.println("🚀 Kafka [Order]: Eventos enviados para o utilizador " + username);
    }
}