package com.xcommerce.payment_service.model;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private Double amount;
    private String cardNumber;
    private String method;
}