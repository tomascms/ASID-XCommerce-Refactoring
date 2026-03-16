package com.xcommerce.payment_service.service;

import com.xcommerce.payment_service.model.PaymentRequest;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PaymentService {
    public String process(PaymentRequest request) {
        if (request.getAmount() > 1000) {
            return "FAILED: Limit exceeded";
        }
        return "SUCCESS: Transaction-" + UUID.randomUUID();
    }
}