package com.xcommerce.notification_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRetryService retryService;

    public NotificationConsumer(NotificationRetryService retryService) {
        this.retryService = retryService;
    }

    // 1. Notificar Novo Produto
    @KafkaListener(topics = "product-events", groupId = "notification-group")
    public void consumeProductEvents(String message) {
        retryService.executeWithBackoff("product notification", () -> deliver("📢 [NOTIFICATION] Newsletter - Novo produto adicionado: {}", message));
    }

    // 2. Notificar Confirmação de Pedido
    @KafkaListener(topics = "order-confirmed-events", groupId = "notification-group")
    public void consumeOrderConfirmedEvents(String message) {
        retryService.executeWithBackoff("order-confirmed notification", () -> deliver("✅ [NOTIFICATION] Order Confirmed - Pedido confirmado: {}", message));
    }

    // 3. Notificar Cancelamento de Pedido
    @KafkaListener(topics = "order-cancelled-events", groupId = "notification-group")
    public void consumeOrderCancelledEvents(String message) {
        retryService.executeWithBackoff("order-cancelled notification", () -> deliver("❌ [NOTIFICATION] Order Cancelled - Pedido cancelado: {}", message));
    }

    private void deliver(String logMessage, String message) {
        if (message != null && (message.contains("SMTP_DOWN") || message.contains("MAIL_SERVER_ERROR"))) {
            throw new IllegalStateException("Temporary SMTP failure");
        }

        if (logMessage.startsWith("❌")) {
            log.warn(logMessage, message);
        } else {
            log.info(logMessage, message);
        }
    }
}