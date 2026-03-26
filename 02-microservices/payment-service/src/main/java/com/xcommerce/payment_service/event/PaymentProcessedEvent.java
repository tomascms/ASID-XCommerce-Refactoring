package com.xcommerce.payment_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PaymentProcessedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Double amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private LocalDateTime processedAt;
    
    public PaymentProcessedEvent() {}
    
    public PaymentProcessedEvent(Long paymentId, Long orderId, Long userId, Double amount, 
                                 String paymentMethod, String status, String transactionId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionId = transactionId;
        this.processedAt = LocalDateTime.now();
    }
    
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    @Override
    public String toString() {
        return "PaymentProcessedEvent{" +
                "paymentId=" + paymentId +
                ", orderId=" + orderId +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", processedAt=" + processedAt +
                '}';
    }
}
