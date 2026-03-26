package com.xcommerce.cart_service.dto;

public class CartItemResponse {
    private Long id;
    private String username;
    private Long productId;
    private Integer quantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public static CartItemResponse from(com.xcommerce.cart_service.model.CartItem item) {
        CartItemResponse dto = new CartItemResponse();
        dto.setId(item.getId());
        dto.setUsername(item.getUsername());
        dto.setProductId(item.getProductId());
        dto.setQuantity(item.getQuantity());
        return dto;
    }
}