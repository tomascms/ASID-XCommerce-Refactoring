package com.xcommerce.inventory_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class InventoryReleasedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long reservationId;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private String releaseReason;
    private LocalDateTime releasedAt;
    
    public InventoryReleasedEvent() {}
    
    public InventoryReleasedEvent(Long reservationId, Long orderId, Long productId, 
                                 Integer quantity, String releaseReason) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.releaseReason = releaseReason;
        this.releasedAt = LocalDateTime.now();
    }
    
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getReleaseReason() { return releaseReason; }
    public void setReleaseReason(String releaseReason) { this.releaseReason = releaseReason; }
    
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
    
    @Override
    public String toString() {
        return "InventoryReleasedEvent{" +
                "reservationId=" + reservationId +
                ", orderId=" + orderId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", releaseReason='" + releaseReason + '\'' +
                ", releasedAt=" + releasedAt +
                '}';
    }
}
