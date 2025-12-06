package com.netfliz.worker.repository;

import com.netfliz.worker.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByUserIdAndStatus(Long userId, String status);

    List<Subscription> findByStatusAndExpiryDateBefore(String status, LocalDateTime expiryDate);
}