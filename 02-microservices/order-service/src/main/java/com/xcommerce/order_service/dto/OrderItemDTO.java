package com.xcommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private Long productId;
    private Integer quantity;

    // Método para parsear de String (ex: "1:2")
    public static OrderItemDTO fromString(String itemString) {
        String[] parts = itemString.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato de item inválido: " + itemString);
        }
        return new OrderItemDTO(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
    }

    @Override
    public String toString() {
        return productId + ":" + quantity;
    }
}