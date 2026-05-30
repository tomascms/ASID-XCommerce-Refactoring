package com.xcommerce.order_service.service;

import com.xcommerce.order_service.model.OrderItem;
import com.xcommerce.order_service.model.OrderOutboxEvent;
import com.xcommerce.order_service.repository.OrderOutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderOutboxEventRepository outboxEventRepository;

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate, OrderOutboxEventRepository outboxEventRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxEventRepository = outboxEventRepository;
    }

    public void sendOrderCreatedEvent(Long orderId, String username, java.util.List<OrderItem> items) {
        String itemsPayload = items.stream()
                .map(item -> item.getProductId() + ":" + item.getQuantity())
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String message = String.format("orderId:%d,username:%s,items:%s", orderId, username, itemsPayload);
        OrderOutboxEvent outboxEvent = new OrderOutboxEvent();
        outboxEvent.setAggregateId(orderId);
        outboxEvent.setEventType("ORDER_CREATED");
        outboxEvent.setPayload(message);
        outboxEventRepository.save(outboxEvent);

        Runnable publish = () -> {
            this.kafkaTemplate.send("order-created-events", String.valueOf(orderId), message);
            log.info("🚀 [ORDER] Evento OrderCreatedEvent enviado para Kafka para orderId={}", orderId);
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    public void sendOrderConfirmedEvent(Long orderId, String username) {
        this.kafkaTemplate.send("order-confirmed-events", String.valueOf(orderId), "ORDER_CONFIRMED:" + username);
        log.info("🚀 [ORDER] Evento OrderConfirmedEvent enviado para Kafka para orderId={}", orderId);
    }
}