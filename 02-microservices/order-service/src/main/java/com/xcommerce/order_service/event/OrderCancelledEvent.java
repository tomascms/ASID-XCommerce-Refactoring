package com.xcommerce.order_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderCancelledEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long orderId;
    private Long userId;
    private String reason;
    private String refundStatus;
    private LocalDateTime cancelledAt;
    
    public OrderCancelledEvent() {}
    
    public OrderCancelledEvent(Long orderId, Long userId, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.refundStatus = "PENDING";
        this.cancelledAt = LocalDateTime.now();
    }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    
    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", reason='" + reason + '\'' +
                ", refundStatus='" + refundStatus + '\'' +
                ", cancelledAt=" + cancelledAt +
                '}';
    }
}
