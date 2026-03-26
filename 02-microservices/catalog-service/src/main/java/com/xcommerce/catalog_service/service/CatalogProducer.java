package com.xcommerce.catalog_service.service;

import com.xcommerce.catalog_service.model.Product;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CatalogProducer {
    private static final Logger log = LoggerFactory.getLogger(CatalogProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CatalogProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProductCreatedEvent(Product product) {
        int quantity = product.getQuantity() != null ? product.getQuantity() : 0;
        String payload = product.getId() + ":" + quantity;
        this.kafkaTemplate.send("product-events", product.getId().toString(), payload);
        log.info("📦 [CATALOG] Evento de produto enviado. Payload: {}", payload);
    }
}
