package com.xcommerce.payment_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
    public void processPayment(
            String message,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String orderIdKey) {
        String orderId = (orderIdKey != null) ? orderIdKey : message;
        log.info("💳 [PAYMENT] Processando pagamento para a encomenda: {} (orderId={})", message, orderId);

        try {
            Thread.sleep(2000);

            boolean paymentSuccessful = new Random().nextDouble() > 0.2;
            String paymentStatus = paymentSuccessful ? "PAYMENT_SUCCESSFUL" : "PAYMENT_FAILED";

            kafkaTemplate.send("payment-events", orderId, paymentStatus);

            if (paymentSuccessful) {
                log.info("✅ [PAYMENT] Pagamento aprovado! orderId={}", orderId);
            } else {
                log.warn("❌ [PAYMENT] Pagamento recusado! orderId={}", orderId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Erro no processamento de pagamento: {}", e.getMessage());
            kafkaTemplate.send("payment-events", orderId, "PAYMENT_FAILED");
        }
    }
}