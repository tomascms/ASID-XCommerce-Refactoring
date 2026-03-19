package com.xcommerce.notification_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    // 1. Notificar Login 
    @KafkaListener(topics = "user-events", groupId = "notification-group")
    public void consumeAuthEvents(String message) {
        System.out.println("🔔 [NOTIFICATION]: Simulação de E-mail enviada!");
        System.out.println("📩 Conteúdo: Olá! Detetámos um novo acesso à sua conta: " + message);
    }

    // 2. Notificar Novo Produto 
    @KafkaListener(topics = "product-events", groupId = "notification-group")
    public void consumeProductEvents(String message) {
        System.out.println("📢 [NOTIFICATION]: Newsletter de Marketing!");
        System.out.println("📩 Conteúdo: Acabou de chegar um novo produto à loja! ID: " + message);
    }

    // 3. Notificar Encomenda 
    @KafkaListener(topics = "order-placed-events", groupId = "notification-group")
    public void consumeOrderEvents(String message) {
        System.out.println("📦 [NOTIFICATION]: Confirmação de Encomenda!");
        System.out.println("📩 Conteúdo: A sua encomenda foi processada com sucesso. Detalhes: " + message);
    }
}