package com.xcommerce.payment_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessor {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentProcessor(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-placed-events", groupId = "payment-group")
    public void processPayment(String message) {
        System.out.println("💳 [PAYMENT]: A processar pagamento para a encomenda: " + message);
        
        try {
            Thread.sleep(2000);
            
            String paymentStatus = "PAYMENT_SUCCESSFUL";
            
            kafkaTemplate.send("payment-events", message, paymentStatus);
            
            System.out.println("✅ [PAYMENT]: Pagamento aprovado! Notificação enviada ao Kafka.");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ Erro no processamento de pagamento: " + e.getMessage());
        }
    }
}