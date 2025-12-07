package com.netfliz.worker.service;

import com.netfliz.worker.entity.analysis.AnalyticsEventEntity;
import com.netfliz.worker.entity.analysis.SearchLog;
import com.netfliz.worker.model.event.AnalyticsEvent;
import com.netfliz.worker.model.event.UserActivityEvent;
import com.netfliz.worker.repository.analysis.AnalyticsEventRepository;
import com.netfliz.worker.repository.analysis.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final SearchLogRepository searchLogRepository;

    /**
     * Xử lý single analytics event
     */
    @Transactional
    public void processEvent(AnalyticsEvent event) {
        try {
            log.debug("Processing analytics event - type: {}, userId: {}",
                    event.getEventType(), event.getUserId());

            AnalyticsEventEntity analyticsEntity = AnalyticsEventEntity.builder()
                    .eventType(event.getEventType())
                    .userId(event.getUserId())
                    .sessionId(event.getSessionId())
                    .page(event.getPage())
                    .action(event.getAction())
                    .properties(serializeProperties(event.getProperties()))
                    .timestamp(event.getTimestamp())
                    .createdAt(LocalDateTime.now())
                    .build();

            analyticsEventRepository.save(analyticsEntity);

            log.debug("✓ Analytics event saved - type: {}", event.getEventType());

        } catch (Exception e) {
            log.error("Failed to process analytics event", e);
            // Không throw exception vì analytics không critical
        }
    }

    /**
     * Xử lý batch analytics events
     * Tối ưu performance khi nhận nhiều events cùng lúc
     */
    @Transactional
    public void processBatchEvents(List<AnalyticsEvent> events) {
        try {
            log.info("Processing analytics batch - size: {}", events.size());

            List<AnalyticsEventEntity> entities = events.stream()
                    .map(event -> AnalyticsEventEntity.builder()
                            .eventType(event.getEventType())
                            .userId(event.getUserId())
                            .sessionId(event.getSessionId())
                            .page(event.getPage())
                            .action(event.getAction())
                            .properties(serializeProperties(event.getProperties()))
                            .timestamp(event.getTimestamp())
                            .createdAt(LocalDateTime.now())
                            .build())
                    .toList();

            // Batch insert
            analyticsEventRepository.saveAll(entities);

            log.info("✓ Analytics batch saved - {} events", entities.size());

        } catch (Exception e) {
            log.error("Failed to process analytics batch", e);
        }
    }

    /**
     * Track search activity
     */
    @Transactional
    public void trackSearch(UserActivityEvent event) {
        try {
            log.debug("Tracking search - userId: {}", event.getUserId());

            // Parse metadata để lấy search query
            String query = extractSearchQuery(event.getMetadata());

            if (query == null || query.isEmpty()) {
                log.warn("Search query is empty");
                return;
            }

            SearchLog searchLog = SearchLog.builder()
                    .userId(event.getUserId())
                    .query(query)
                    .timestamp(event.getTimestamp())
                    .resultsCount(0)  // TODO: Add results count
                    .build();

            searchLogRepository.save(searchLog);

            log.debug("✓ Search tracked - userId: {}, query: '{}'",
                    event.getUserId(), query);

        } catch (Exception e) {
            log.error("Failed to track search", e);
        }
    }

    /**
     * Get analytics summary cho dashboard
     */
    public Map<String, Object> getAnalyticsSummary(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            log.debug("Getting analytics summary - from {} to {}", startDate, endDate);

            // Total events
            long totalEvents = analyticsEventRepository
                    .countByTimestampBetween(startDate, endDate);

            // Unique users
            long uniqueUsers = analyticsEventRepository
                    .countDistinctUsersByTimestampBetween(startDate, endDate);

            // Top events
            List<Map<String, Object>> topEvents = analyticsEventRepository
                    .findTopEventTypes(startDate, endDate, 10);

            // Top pages
            List<Map<String, Object>> topPages = analyticsEventRepository
                    .findTopPages(startDate, endDate, 10);

            // Top searches
            List<Map<String, Object>> topSearches = searchLogRepository
                    .findTopSearches(startDate, endDate, 10);

            Map<String, Object> summary = Map.of(
                    "totalEvents", totalEvents,
                    "uniqueUsers", uniqueUsers,
                    "topEvents", topEvents,
                    "topPages", topPages,
                    "topSearches", topSearches,
                    "dateRange", Map.of(
                            "start", startDate,
                            "end", endDate
                    )
            );

            log.debug("✓ Analytics summary generated - {} events, {} users",
                    totalEvents, uniqueUsers);

            return summary;

        } catch (Exception e) {
            log.error("Failed to get analytics summary", e);
            throw new RuntimeException("Failed to get analytics summary", e);
        }
    }

    /**
     * Get user behavior analytics
     */
    public Map<String, Object> getUserBehaviorAnalytics(Long userId, int days) {
        try {
            log.debug("Getting user behavior analytics - userId: {}, days: {}", userId, days);

            LocalDateTime startDate = LocalDateTime.now().minusDays(days);

            // User events
            List<AnalyticsEventEntity> events = analyticsEventRepository
                    .findByUserIdAndTimestampAfter(userId, startDate);

            // Session count
            long sessionCount = events.stream()
                    .map(AnalyticsEventEntity::getSessionId)
                    .distinct()
                    .count();

            // Most visited pages
            Map<String, Long> pageVisits = events.stream()
                    .filter(e -> e.getPage() != null)
                    .collect(Collectors.groupingBy(
                            AnalyticsEventEntity::getPage,
                            Collectors.counting()
                    ));

            // Most common actions
            Map<String, Long> actions = events.stream()
                    .filter(e -> e.getAction() != null)
                    .collect(Collectors.groupingBy(
                            AnalyticsEventEntity::getAction,
                            Collectors.counting()
                    ));

            Map<String, Object> behavior = Map.of(
                    "userId", userId,
                    "days", days,
                    "totalEvents", events.size(),
                    "sessionCount", sessionCount,
                    "pageVisits", pageVisits,
                    "actions", actions
            );

            log.debug("✓ User behavior analytics generated - userId: {}", userId);

            return behavior;

        } catch (Exception e) {
            log.error("Failed to get user behavior analytics", e);
            throw new RuntimeException("Failed to get user behavior analytics", e);
        }
    }

    /**
     * Track conversion events
     */
    @Transactional
    public void trackConversion(Long userId, String conversionType, Map<String, Object> data) {
        try {
            log.info("Tracking conversion - userId: {}, type: {}", userId, conversionType);

            AnalyticsEventEntity event = AnalyticsEventEntity.builder()
                    .eventType("CONVERSION")
                    .userId(userId)
                    .action(conversionType)
                    .properties(serializeProperties(data))
                    .timestamp(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            analyticsEventRepository.save(event);

            log.info("✓ Conversion tracked - userId: {}, type: {}", userId, conversionType);

        } catch (Exception e) {
            log.error("Failed to track conversion", e);
        }
    }

    /**
     * Get real-time metrics
     */
    public Map<String, Object> getRealTimeMetrics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fiveMinutesAgo = now.minusMinutes(5);

            long activeUsers = analyticsEventRepository
                    .countDistinctUsersByTimestampBetween(fiveMinutesAgo, now);

            long recentEvents = analyticsEventRepository
                    .countByTimestampBetween(fiveMinutesAgo, now);

            List<Map<String, Object>> recentActions = analyticsEventRepository
                    .findRecentActions(fiveMinutesAgo, 20);

            Map<String, Object> metrics = Map.of(
                    "activeUsers", activeUsers,
                    "recentEvents", recentEvents,
                    "recentActions", recentActions,
                    "timestamp", now
            );

            log.debug("✓ Real-time metrics generated - {} active users", activeUsers);

            return metrics;

        } catch (Exception e) {
            log.error("Failed to get real-time metrics", e);
            throw new RuntimeException("Failed to get real-time metrics", e);
        }
    }

    /**
     * Serialize properties map to JSON string
     */
    private String serializeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }

        try {
            // Using simple JSON serialization
            // In production, use Jackson ObjectMapper
            return properties.toString();
        } catch (Exception e) {
            log.warn("Failed to serialize properties", e);
            return "{}";
        }
    }

    /**
     * Extract search query from metadata JSON
     */
    private String extractSearchQuery(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            // Simple JSON parsing
            // In production, use Jackson ObjectMapper
            if (metadata.contains("\"query\"")) {
                int start = metadata.indexOf("\"query\":\"") + 9;
                int end = metadata.indexOf("\"", start);
                return metadata.substring(start, end);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract search query from metadata", e);
            return null;
        }
    }
}
