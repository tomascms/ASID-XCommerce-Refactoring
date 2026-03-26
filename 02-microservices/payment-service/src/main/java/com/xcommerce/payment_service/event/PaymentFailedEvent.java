package com.xcommerce.payment_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PaymentFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private Double amount;
    private String failureReason;
    private String errorCode;
    private LocalDateTime failedAt;
    
    public PaymentFailedEvent() {}
    
    public PaymentFailedEvent(Long paymentId, Long orderId, Long userId, Double amount, 
                             String failureReason, String errorCode) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.failureReason = failureReason;
        this.errorCode = errorCode;
        this.failedAt = LocalDateTime.now();
    }
    
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    
    @Override
    public String toString() {
        return "PaymentFailedEvent{" +
                "paymentId=" + paymentId +
                ", orderId=" + orderId +
                ", amount=" + amount +
                ", failureReason='" + failureReason + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", failedAt=" + failedAt +
                '}';
    }
}
