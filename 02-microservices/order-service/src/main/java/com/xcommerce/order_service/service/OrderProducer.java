package com.xcommerce.order_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderEvents(String username, Long productId, Integer quantity) {
        this.kafkaTemplate.send("order-placed", username);

        String inventoryMessage = productId + ":" + quantity;
        this.kafkaTemplate.send("order-placed-events", inventoryMessage);

        log.info("🚀 [ORDER] Eventos enviados para Kafka pelo utilizador: {}", username);
    }
}