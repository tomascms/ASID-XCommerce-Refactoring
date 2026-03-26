package com.xcommerce.order_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderPaymentListener {
    private static final Logger log = LoggerFactory.getLogger(OrderPaymentListener.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderPaymentListener(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @KafkaListener(topics = "payment-events", groupId = "order-group")
    public void handlePaymentEvent(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 2) return;
            
            String paymentId = parts[0];
            String status = parts[1];
            
            if ("PAYMENT_SUCCESSFUL".equals(status)) {
                log.info("✅ [ORDER] Pagamento confirmado: {}. Confirmando pedido...", paymentId);
                
                // Publish order-confirmed event
                kafkaTemplate.send("order-confirmed-events", paymentId, "ORDER_CONFIRMED:" + paymentId);
                
            } else if ("PAYMENT_FAILED".equals(status)) {
                log.warn("❌ [ORDER] Pagamento falhou: {}. Cancelando pedido...", paymentId);
                
                // Publish order-cancelled event
                kafkaTemplate.send("order-cancelled-events", paymentId, "ORDER_CANCELLED:" + paymentId);
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage());
        }
    }
}
