package com.xcommerce.payment_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;

@Service
public class PaymentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentProcessor(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-placed-events", groupId = "payment-group")
    public void processPayment(String message) {
        log.info("💳 [PAYMENT] Processando pagamento para a encomenda: {}", message);
        
        try {
            Thread.sleep(2000);
            
            // Simular falha aleatória (~20% de chance)
            boolean paymentSuccessful = new Random().nextDouble() > 0.2;
            
            String paymentStatus = paymentSuccessful ? "PAYMENT_SUCCESSFUL" : "PAYMENT_FAILED";
            
            kafkaTemplate.send("payment-events", message, paymentStatus);
            
            if (paymentSuccessful) {
                log.info("✅ [PAYMENT] Pagamento aprovado! Notificando Order Service...");
            } else {
                log.warn("❌ [PAYMENT] Pagamento recusado! Notificando Order Service para cancelamento...");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Erro no processamento de pagamento: {}", e.getMessage());
            // Publish payment failed event on exception
            kafkaTemplate.send("payment-events", message, "PAYMENT_FAILED");
        }
    }
}