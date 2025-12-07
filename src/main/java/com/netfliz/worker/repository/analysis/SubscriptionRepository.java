package com.netfliz.worker.repository.analysis;

import com.netfliz.worker.entity.analysis.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByUserIdAndStatus(Long userId, String status);

    List<Subscription> findByStatusAndExpiryDateBefore(String status, LocalDateTime expiryDate);
}