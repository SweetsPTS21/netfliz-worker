package com.netfliz.worker.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String transactionId;
    private Long userId;
    private String subscriptionType; // BASIC, PREMIUM, ULTRA
    private Double amount;
    private String currency;
    private String status; // PENDING, COMPLETED, FAILED
    private String paymentMethod;
    private LocalDateTime timestamp;
}
