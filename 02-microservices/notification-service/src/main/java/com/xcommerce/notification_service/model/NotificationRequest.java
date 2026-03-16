package com.xcommerce.notification_service.model;

import lombok.Data;

@Data
public class NotificationRequest {
    private String email;
    private String message;
    private String type;
}