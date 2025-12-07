package com.netfliz.worker.service;

import com.netfliz.worker.entity.analysis.Payment;
import com.netfliz.worker.entity.analysis.Subscription;
import com.netfliz.worker.entity.analysis.User;
import com.netfliz.worker.model.event.PaymentEvent;
import com.netfliz.worker.repository.analysis.PaymentRepository;
import com.netfliz.worker.repository.analysis.SubscriptionRepository;
import com.netfliz.worker.repository.analysis.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    /**
     * Xử lý payment pending
     * Lưu thông tin payment và đợi confirmation
     */
    @Transactional
    public void processPendingPayment(PaymentEvent event) {
        try {
            log.info("Processing pending payment - transactionId: {}, userId: {}",
                    event.getTransactionId(), event.getUserId());

            // Check if payment already exists
            Optional<Payment> existingPayment = paymentRepository
                    .findByTransactionId(event.getTransactionId());

            if (existingPayment.isPresent()) {
                log.warn("Payment already exists - transactionId: {}", event.getTransactionId());
                return;
            }

            // Create payment record
            Payment payment = Payment.builder()
                    .transactionId(event.getTransactionId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .subscriptionType(event.getSubscriptionType())
                    .paymentMethod(event.getPaymentMethod())
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);

            log.info("✓ Pending payment saved - transactionId: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to process pending payment - transactionId: {}",
                    event.getTransactionId(), e);
            throw e;
        }
    }

    /**
     * Complete payment
     * Update payment status và activate subscription
     */
    @Transactional
    public void completePayment(PaymentEvent event) {
        try {
            log.info("Completing payment - transactionId: {}, userId: {}",
                    event.getTransactionId(), event.getUserId());

            // Update payment status
            Payment payment = paymentRepository.findByTransactionId(event.getTransactionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Payment not found: " + event.getTransactionId()));

            payment.setStatus("COMPLETED");
            payment.setCompletedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            log.info("✓ Payment completed - transactionId: {}, amount: {} {}",
                    event.getTransactionId(), event.getAmount(), event.getCurrency());

        } catch (Exception e) {
            log.error("CRITICAL: Failed to complete payment - transactionId: {}",
                    event.getTransactionId(), e);
            throw e;
        }
    }

    /**
     * Handle failed payment
     */
    @Transactional
    public void handleFailedPayment(PaymentEvent event) {
        try {
            log.warn("Handling failed payment - transactionId: {}, userId: {}",
                    event.getTransactionId(), event.getUserId());

            // Update payment status
            Payment payment = paymentRepository.findByTransactionId(event.getTransactionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Payment not found: " + event.getTransactionId()));

            payment.setStatus("FAILED");
            payment.setFailedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            // TODO: Implement retry logic hoặc suspend account
            // checkSubscriptionStatus(event.getUserId());

            log.warn("✗ Payment failed - transactionId: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to handle payment failure - transactionId: {}",
                    event.getTransactionId(), e);
            throw e;
        }
    }

    /**
     * Update user subscription
     */
    @Transactional
    public void updateUserSubscription(Long userId, String subscriptionType) {
        try {
            log.info("Updating subscription - userId: {}, type: {}", userId, subscriptionType);

            // Get or create subscription
            Optional<Subscription> existingSub = subscriptionRepository.findByUserId(userId);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiryDate = calculateExpiryDate(subscriptionType, now);

            Subscription subscription;
            if (existingSub.isPresent()) {
                subscription = existingSub.get();
                subscription.setType(subscriptionType);
                subscription.setStatus("ACTIVE");
                subscription.setStartDate(now);
                subscription.setExpiryDate(expiryDate);
                subscription.setUpdatedAt(now);
            } else {
                subscription = Subscription.builder()
                        .userId(userId)
                        .type(subscriptionType)
                        .status("ACTIVE")
                        .startDate(now)
                        .expiryDate(expiryDate)
                        .autoRenew(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
            }

            subscriptionRepository.save(subscription);

            // Update user premium status
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            user.setIsPremium(true);
            user.setSubscriptionType(subscriptionType);
            userRepository.save(user);

            log.info("✓ Subscription updated - userId: {}, type: {}, expires: {}",
                    userId, subscriptionType, expiryDate);

        } catch (Exception e) {
            log.error("CRITICAL: Failed to update subscription - userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * Check subscription status
     * Gọi định kỳ để kiểm tra expired subscriptions
     */
    @Transactional
    public void checkAndUpdateExpiredSubscriptions() {
        try {
            log.info("Checking expired subscriptions");

            LocalDateTime now = LocalDateTime.now();

            // Find expired subscriptions
            java.util.List<Subscription> expiredSubs = subscriptionRepository
                    .findByStatusAndExpiryDateBefore("ACTIVE", now);

            for (Subscription sub : expiredSubs) {
                if (sub.isAutoRenew()) {
                    log.info("Auto-renewing subscription for userId: {}", sub.getUserId());
                    // TODO: Trigger payment renewal
                    renewSubscription(sub);
                } else {
                    log.info("Expiring subscription for userId: {}", sub.getUserId());
                    expireSubscription(sub);
                }
            }

            log.info("✓ Processed {} expired subscriptions", expiredSubs.size());

        } catch (Exception e) {
            log.error("Failed to check expired subscriptions", e);
        }
    }

    /**
     * Renew subscription
     */
    @Transactional
    private void renewSubscription(Subscription subscription) {
        try {
            log.info("Renewing subscription for userId: {}", subscription.getUserId());

            // TODO: Process payment for renewal
            // For now, just extend the subscription

            LocalDateTime newExpiryDate = calculateExpiryDate(
                    subscription.getType(),
                    subscription.getExpiryDate()
            );

            subscription.setExpiryDate(newExpiryDate);
            subscription.setUpdatedAt(LocalDateTime.now());

            subscriptionRepository.save(subscription);

            log.info("✓ Subscription renewed - userId: {}, new expiry: {}",
                    subscription.getUserId(), newExpiryDate);

        } catch (Exception e) {
            log.error("Failed to renew subscription for userId: {}",
                    subscription.getUserId(), e);
        }
    }

    /**
     * Expire subscription
     */
    @Transactional
    private void expireSubscription(Subscription subscription) {
        try {
            log.info("Expiring subscription for userId: {}", subscription.getUserId());

            subscription.setStatus("EXPIRED");
            subscription.setUpdatedAt(LocalDateTime.now());

            subscriptionRepository.save(subscription);

            // Update user premium status
            User user = userRepository.findById(subscription.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "User not found: " + subscription.getUserId()));
            user.setIsPremium(false);
            user.setSubscriptionType("FREE");
            userRepository.save(user);

            log.info("✓ Subscription expired - userId: {}", subscription.getUserId());

        } catch (Exception e) {
            log.error("Failed to expire subscription for userId: {}",
                    subscription.getUserId(), e);
        }
    }

    /**
     * Calculate subscription expiry date
     */
    private LocalDateTime calculateExpiryDate(String subscriptionType, LocalDateTime startDate) {
        return switch (subscriptionType) {
            case "BASIC" -> startDate.plusMonths(1);
            case "PREMIUM" -> startDate.plusMonths(1);
            case "ULTRA" -> startDate.plusMonths(1);
            case "YEARLY_BASIC" -> startDate.plusYears(1);
            case "YEARLY_PREMIUM" -> startDate.plusYears(1);
            case "YEARLY_ULTRA" -> startDate.plusYears(1);
            default -> startDate.plusMonths(1);
        };
    }

    /**
     * Get payment history for user
     */
    public java.util.List<Payment> getPaymentHistory(Long userId) {
        log.debug("Fetching payment history for userId: {}", userId);
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get current subscription for user
     */
    public Optional<Subscription> getCurrentSubscription(Long userId) {
        log.debug("Fetching current subscription for userId: {}", userId);
        return subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    /**
     * Cancel subscription
     */
    @Transactional
    public void cancelSubscription(Long userId) {
        try {
            log.info("Canceling subscription for userId: {}", userId);

            Subscription subscription = subscriptionRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Subscription not found"));

            subscription.setAutoRenew(false);
            subscription.setUpdatedAt(LocalDateTime.now());

            subscriptionRepository.save(subscription);

            log.info("✓ Subscription canceled - userId: {}, expires: {}",
                    userId, subscription.getExpiryDate());

        } catch (Exception e) {
            log.error("Failed to cancel subscription for userId: {}", userId, e);
            throw e;
        }
    }
}