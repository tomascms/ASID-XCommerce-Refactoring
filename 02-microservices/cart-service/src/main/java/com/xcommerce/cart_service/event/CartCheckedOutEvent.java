package com.xcommerce.cart_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class CartCheckedOutEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long cartId;
    private Long userId;
    private List<CartItem> items;
    private Double totalAmount;
    private LocalDateTime checkedOutAt;
    
    public CartCheckedOutEvent() {}
    
    public CartCheckedOutEvent(Long cartId, Long userId, List<CartItem> items, Double totalAmount) {
        this.cartId = cartId;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.checkedOutAt = LocalDateTime.now();
    }
    
    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }
    
    @Override
    public String toString() {
        return "CartCheckedOutEvent{" +
                "cartId=" + cartId +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", checkedOutAt=" + checkedOutAt +
                '}';
    }
    
    public static class CartItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long productId;
        private Integer quantity;
        private Double price;
        
        public CartItem() {}
        
        public CartItem(Long productId, Integer quantity, Double price) {
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
