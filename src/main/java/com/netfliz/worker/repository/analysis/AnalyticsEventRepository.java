package com.netfliz.worker.repository.analysis;

import com.netfliz.worker.entity.analysis.AnalyticsEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, Long> {

    List<AnalyticsEventEntity> findByUserIdAndTimestampAfter(Long userId, LocalDateTime timestamp);

    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT ae.userId) FROM AnalyticsEventEntity ae WHERE ae.timestamp BETWEEN :start AND :end")
    long countDistinctUsersByTimestampBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = "SELECT event_type as eventType, COUNT(*) as count FROM analytics_events " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY event_type ORDER BY count DESC LIMIT :limit",
            nativeQuery = true)
    List<Map<String, Object>> findTopEventTypes(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );

    @Query(value = "SELECT page, COUNT(*) as count FROM analytics_events " +
            "WHERE timestamp BETWEEN :start AND :end AND page IS NOT NULL " +
            "GROUP BY page ORDER BY count DESC LIMIT :limit",
            nativeQuery = true)
    List<Map<String, Object>> findTopPages(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );

    @Query(value = "SELECT event_type, action, user_id, timestamp FROM analytics_events " +
            "WHERE timestamp >= :since ORDER BY timestamp DESC LIMIT :limit",
            nativeQuery = true)
    List<Map<String, Object>> findRecentActions(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit
    );
}