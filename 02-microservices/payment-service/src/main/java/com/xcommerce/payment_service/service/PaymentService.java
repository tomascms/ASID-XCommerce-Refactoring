package com.xcommerce.payment_service.service;

import com.xcommerce.payment_service.model.PaymentRequest;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PaymentService {
    public String process(PaymentRequest request) {
        // Validar request
        if (request == null) {
            return "FAILED: Invalid request";
        }
        
        // Validar amount
        if (request.getAmount() == null || request.getAmount() <= 0) {
            return "FAILED: Invalid amount";
        }
        
        // Verificar limite
        if (request.getAmount() > 1000) {
            return "FAILED: Limit exceeded";
        }
        
        return "SUCCESS: Transaction-" + UUID.randomUUID();
    }
}