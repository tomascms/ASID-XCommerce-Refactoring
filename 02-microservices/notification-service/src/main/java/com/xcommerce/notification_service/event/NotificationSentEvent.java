package com.xcommerce.notification_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class NotificationSentEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long notificationId;
    private Long orderId;
    private Long userId;
    private String notificationType;
    private String recipient;
    private String channel;
    private String subject;
    private String message;
    private LocalDateTime sentAt;
    
    public NotificationSentEvent() {}
    
    public NotificationSentEvent(Long notificationId, Long orderId, Long userId, 
                                 String notificationType, String recipient, String channel) {
        this.notificationId = notificationId;
        this.orderId = orderId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.channel = channel;
        this.sentAt = LocalDateTime.now();
    }
    
    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    @Override
    public String toString() {
        return "NotificationSentEvent{" +
                "notificationId=" + notificationId +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", notificationType='" + notificationType + '\'' +
                ", channel='" + channel + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}
