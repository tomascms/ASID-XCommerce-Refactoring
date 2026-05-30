package com.xcommerce.payment_service.controller;

import com.xcommerce.payment_service.model.PaymentRequest;
import com.xcommerce.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/process")
    public String processPayment(@RequestBody PaymentRequest request) {
        return paymentService.process(request);
    }
}