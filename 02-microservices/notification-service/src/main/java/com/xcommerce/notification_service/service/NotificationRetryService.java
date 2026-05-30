package com.xcommerce.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationRetryService {
    private static final Logger log = LoggerFactory.getLogger(NotificationRetryService.class);

    public void executeWithBackoff(String description, Runnable action) {
        int maxAttempts = 5;
        long delayMillis = 1000L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.run();
                return;
            } catch (RuntimeException exception) {
                if (attempt == maxAttempts) {
                    throw exception;
                }

                log.warn("⚠️ [NOTIFICATION] {} failed on attempt {}/{}: {}", description, attempt, maxAttempts, exception.getMessage());
                sleep(delayMillis);
                delayMillis = Math.min(delayMillis * 2, 8000L);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", interruptedException);
        }
    }
}