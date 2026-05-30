package com.xcommerce.cart_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CartItemAddedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long cartId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Double price;
    private LocalDateTime addedAt;
    
    public CartItemAddedEvent() {}
    
    public CartItemAddedEvent(Long cartId, Long userId, Long productId, Integer quantity, Double price) {
        this.cartId = cartId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.addedAt = LocalDateTime.now();
    }
    
    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    
    @Override
    public String toString() {
        return "CartItemAddedEvent{" +
                "cartId=" + cartId +
                ", userId=" + userId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", price=" + price +
                ", addedAt=" + addedAt +
                '}';
    }
}
