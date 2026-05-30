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

    // 2. Notificar Encomenda
    @KafkaListener(topics = "order-placed-events", groupId = "notification-group")
    public void consumeOrderEvents(String message) {
        retryService.executeWithBackoff("order notification", () -> deliver("📦 [NOTIFICATION] Order Confirmation - Pedido criado: {}", message));
    }

    // 3. Notificar Pagamento Bem-Sucedido
    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvents(String message) {
        retryService.executeWithBackoff("payment notification", () -> {
            if (message.contains("PAYMENT_SUCCESSFUL")) {
                deliver("✅ [NOTIFICATION] Payment Success - Pagamento confirmado: {}", message);
            } else if (message.contains("PAYMENT_FAILED")) {
                deliver("❌ [NOTIFICATION] Payment Failed - Pagamento recusado: {}. Enviando alerta ao cliente...", message);
            }
        });
    }

    // 4. Notificar Confirmação de Pedido
    @KafkaListener(topics = "order-confirmed-events", groupId = "notification-group")
    public void consumeOrderConfirmedEvents(String message) {
        retryService.executeWithBackoff("order-confirmed notification", () -> deliver("✅ [NOTIFICATION] Order Confirmed - Pedido confirmado após pagamento: {}", message));
    }

    // 5. Notificar Cancelamento de Pedido
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