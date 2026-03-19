package com.xcommerce.catalog_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CatalogProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProductCreatedEvent(String productId) {
        this.kafkaTemplate.send("product-events", productId, "PRODUCT_CREATED");
        System.out.println("🏷️ Kafka [Catalog]: Evento de produto enviado. ID: " + productId);
    }
}