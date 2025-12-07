package com.netfliz.worker.repository.analysis;

import com.netfliz.worker.entity.analysis.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalse(Long userId);

    Long countByUserIdAndReadFalse(Long userId);
}