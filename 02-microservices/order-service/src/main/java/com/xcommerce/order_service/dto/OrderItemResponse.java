package com.xcommerce.order_service.dto;

import com.xcommerce.order_service.model.OrderItem;

public class OrderItemResponse {
    private Long productId;
    private Integer quantity;
    private double price;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public static OrderItemResponse from(OrderItem item) {
        OrderItemResponse dto = new OrderItemResponse();
        dto.setProductId(item.getProductId());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        return dto;
    }
}
