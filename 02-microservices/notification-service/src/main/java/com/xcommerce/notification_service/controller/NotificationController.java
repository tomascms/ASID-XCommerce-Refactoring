package com.xcommerce.notification_service.controller;

import com.xcommerce.notification_service.model.NotificationRequest;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping("/send")
    public String sendNotification(@RequestBody NotificationRequest request) {
        log.info("📧 Enviando notificação para: {}", request.getEmail());
        log.info("Message: {}", request.getMessage());
        return "Notification sent successfully to " + request.getEmail();
    }
}