package com.xcommerce.notification_service.controller;

import com.xcommerce.notification_service.model.NotificationRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @PostMapping("/send")
    public String sendNotification(@RequestBody NotificationRequest request) {
        System.out.println("ENVIANDO EMAIL PARA: " + request.getEmail());
        System.out.println("MENSAGEM: " + request.getMessage());
        return "Notification sent successfully to " + request.getEmail();
    }
}