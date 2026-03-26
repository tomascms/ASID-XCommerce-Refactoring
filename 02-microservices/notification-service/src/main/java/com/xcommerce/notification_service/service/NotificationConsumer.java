package com.xcommerce.notification_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    // 1. Notificar Login 
    @KafkaListener(topics = "user-events", groupId = "notification-group")
    public void consumeAuthEvents(String message) {
        log.info("📧 [NOTIFICATION] Login Alert - Enviando confirmação: {}", message);
    }

    // 2. Notificar Novo Produto 
    @KafkaListener(topics = "product-events", groupId = "notification-group")
    public void consumeProductEvents(String message) {
        log.info("📢 [NOTIFICATION] Newsletter - Novo produto adicionado: {}", message);
    }

    // 3. Notificar Encomenda 
    @KafkaListener(topics = "order-placed-events", groupId = "notification-group")
    public void consumeOrderEvents(String message) {
        log.info("📦 [NOTIFICATION] Order Confirmation - Pedido criado: {}", message);
    }
    
    // 4. Notificar Pagamento Bem-Sucedido
    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvents(String message) {
        if (message.contains("PAYMENT_SUCCESSFUL")) {
            log.info("✅ [NOTIFICATION] Payment Success - Pagamento confirmado: {}", message);
        } else if (message.contains("PAYMENT_FAILED")) {
            log.warn("❌ [NOTIFICATION] Payment Failed - Pagamento recusado: {}. Enviando alerta ao cliente...", message);
        }
    }
    
    // 5. Notificar Confirmação de Pedido
    @KafkaListener(topics = "order-confirmed-events", groupId = "notification-group")
    public void consumeOrderConfirmedEvents(String message) {
        log.info("✅ [NOTIFICATION] Order Confirmed - Pedido confirmado após pagamento: {}", message);
    }
    
    // 6. Notificar Cancelamento de Pedido
    @KafkaListener(topics = "order-cancelled-events", groupId = "notification-group")
    public void consumeOrderCancelledEvents(String message) {
        log.warn("❌ [NOTIFICATION] Order Cancelled - Pedido cancelado: {}", message);
    }
}