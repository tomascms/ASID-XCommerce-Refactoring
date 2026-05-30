package com.xcommerce.order_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long orderId;
    private Long userId;
    private List<OrderLineItem> lineItems;
    private String shippingAddress;
    private String billingAddress;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private String status;
    
    public OrderCreatedEvent() {}
    
    public OrderCreatedEvent(Long orderId, Long userId, List<OrderLineItem> lineItems, 
                            String shippingAddress, String billingAddress, Double totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.lineItems = lineItems;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.totalAmount = totalAmount;
        this.createdAt = LocalDateTime.now();
        this.status = "CREATED";
    }
    
    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public List<OrderLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<OrderLineItem> lineItems) { this.lineItems = lineItems; }
    
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
    
    public static class OrderLineItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long productId;
        private Integer quantity;
        private Double price;
        
        public OrderLineItem() {}
        
        public OrderLineItem(Long productId, Integer quantity, Double price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }
        
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }
}
