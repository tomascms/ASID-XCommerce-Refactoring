package com.xcommerce.order_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderConfirmedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long orderId;
    private Long userId;
    private String orderNumber;
    private Double totalAmount;
    private String status;
    private LocalDateTime confirmedAt;
    
    public OrderConfirmedEvent() {}
    
    public OrderConfirmedEvent(Long orderId, Long userId, String orderNumber, Double totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = "CONFIRMED";
        this.confirmedAt = LocalDateTime.now();
    }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    
    @Override
    public String toString() {
        return "OrderConfirmedEvent{" +
                "orderId=" + orderId +
                ", orderNumber='" + orderNumber + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", confirmedAt=" + confirmedAt +
                '}';
    }
}
