package com.xcommerce.inventory_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class InventoryReservedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long reservationId;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private String reservationStatus;
    private LocalDateTime reservedAt;
    private LocalDateTime expiryTime;
    
    public InventoryReservedEvent() {}
    
    public InventoryReservedEvent(Long reservationId, Long orderId, Long productId, Integer quantity) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.reservationStatus = "RESERVED";
        this.reservedAt = LocalDateTime.now();
        this.expiryTime = this.reservedAt.plusHours(1);
    }
    
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getReservationStatus() { return reservationStatus; }
    public void setReservationStatus(String reservationStatus) { this.reservationStatus = reservationStatus; }
    
    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }
    
    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
    
    @Override
    public String toString() {
        return "InventoryReservedEvent{" +
                "reservationId=" + reservationId +
                ", orderId=" + orderId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", reservationStatus='" + reservationStatus + '\'' +
                ", reservedAt=" + reservedAt +
                ", expiryTime=" + expiryTime +
                '}';
    }
}
