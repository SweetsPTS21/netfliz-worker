package com.netfliz.worker.service;

import com.netfliz.worker.model.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> standardTemplate;
    private final KafkaTemplate<String, Object> highPriorityTemplate;
    private final KafkaTemplate<String, Object> analyticsTemplate;

    @Value("${kafka.topics.video-view}")
    private String videoViewTopic;

    @Value("${kafka.topics.user-activity}")
    private String userActivityTopic;

    @Value("${kafka.topics.recommendation}")
    private String recommendationTopic;

    @Value("${kafka.topics.notification}")
    private String notificationTopic;

    @Value("${kafka.topics.video-progress}")
    private String videoProgressTopic;

    @Value("${kafka.topics.payment}")
    private String paymentTopic;

    @Value("${kafka.topics.analytics}")
    private String analyticsTopic;

    public KafkaProducerService(
            @Qualifier("kafkaTemplate") KafkaTemplate<String, Object> standardTemplate,
            @Qualifier("highPriorityKafkaTemplate") KafkaTemplate<String, Object> highPriorityTemplate,
            @Qualifier("analyticsKafkaTemplate") KafkaTemplate<String, Object> analyticsTemplate) {
        this.standardTemplate = standardTemplate;
        this.highPriorityTemplate = highPriorityTemplate;
        this.analyticsTemplate = analyticsTemplate;
    }

    /**
     * Gửi event xem video
     * Sử dụng userId làm key để đảm bảo messages từ cùng user vào cùng partition
     */
    public CompletableFuture<SendResult<String, Object>> sendVideoViewEvent(VideoViewEvent event) {
        event.setEventId(UUID.randomUUID().toString());

        log.info("Sending video view event - userId: {}, videoId: {}",
                event.getUserId(), event.getVideoId());

        return sendMessage(
                standardTemplate,
                videoViewTopic,
                event.getUserId().toString(),
                event,
                "VideoView"
        );
    }

    /**
     * Gửi event tiến độ xem video
     * Key = userId + videoId để đảm bảo ordering cho cùng user-video
     */
    public CompletableFuture<SendResult<String, Object>> sendVideoProgressEvent(VideoProgressEvent event) {
        String key = event.getUserId() + "-" + event.getVideoId();

        log.debug("Sending video progress - userId: {}, videoId: {}, position: {}s",
                event.getUserId(), event.getVideoId(), event.getCurrentPosition());

        return sendMessage(
                standardTemplate,
                videoProgressTopic,
                key,
                event,
                "VideoProgress"
        );
    }

    /**
     * Gửi event hoạt động user
     */
    public CompletableFuture<SendResult<String, Object>> sendUserActivityEvent(UserActivityEvent event) {
        event.setEventId(UUID.randomUUID().toString());

        log.info("Sending user activity - userId: {}, type: {}",
                event.getUserId(), event.getActivityType());

        return sendMessage(
                standardTemplate,
                userActivityTopic,
                event.getUserId().toString(),
                event,
                "UserActivity"
        );
    }

    /**
     * Gửi event gợi ý phim
     */
    public CompletableFuture<SendResult<String, Object>> sendRecommendationEvent(RecommendationEvent event) {
        log.debug("Sending recommendation - userId: {}, videoId: {}, type: {}",
                event.getUserId(), event.getVideoId(), event.getRecommendationType());

        return sendMessage(
                standardTemplate,
                recommendationTopic,
                event.getUserId().toString(),
                event,
                "Recommendation"
        );
    }

    /**
     * Gửi thông báo
     * Sử dụng nhiều thread consumer nên có thể gửi nhanh
     */
    public CompletableFuture<SendResult<String, Object>> sendNotificationEvent(NotificationEvent event) {
        event.setNotificationId(UUID.randomUUID().toString());

        log.info("Sending notification - userId: {}, type: {}",
                event.getUserId(), event.getType());

        return sendMessage(
                standardTemplate,
                notificationTopic,
                event.getUserId().toString(),
                event,
                "Notification"
        );
    }

    /**
     * Gửi event thanh toán
     * Sử dụng HIGH PRIORITY template với reliability cao nhất
     */
    public CompletableFuture<SendResult<String, Object>> sendPaymentEvent(PaymentEvent event) {
        log.warn("Sending CRITICAL payment event - transactionId: {}, userId: {}, amount: {}",
                event.getTransactionId(), event.getUserId(), event.getAmount());

        CompletableFuture<SendResult<String, Object>> future = sendMessage(
                highPriorityTemplate,  // Dùng high priority template
                paymentTopic,
                event.getTransactionId(),
                event,
                "Payment"
        );

        // Thêm callback đặc biệt cho payment
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✓ Payment event sent successfully - transactionId: {}, partition: {}, offset: {}",
                        event.getTransactionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("✗ CRITICAL: Failed to send payment event - transactionId: {}",
                        event.getTransactionId(), ex);

                // TODO: Có thể trigger alert, retry logic, hoặc fallback mechanism
                alertPaymentFailure(event, ex);
            }
        });

        return future;
    }

    /**
     * Gửi event analytics
     * Sử dụng ANALYTICS template với throughput cao, reliability thấp hơn
     */
    public void sendAnalyticsEvent(AnalyticsEvent event) {
        log.debug("Sending analytics event - type: {}, userId: {}",
                event.getEventType(), event.getUserId());

        // Fire and forget cho analytics - không cần đợi response
        analyticsTemplate.send(analyticsTopic, event.getSessionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to send analytics event (non-critical): {}", ex.getMessage());
                    }
                });
    }

    /**
     * Gửi multiple events cùng lúc (batch)
     * Hữu ích khi cần gửi nhiều events liên quan
     */
    public void sendBatchEvents(VideoViewEvent viewEvent, UserActivityEvent activityEvent) {
        log.info("Sending batch events for userId: {}", viewEvent.getUserId());

        CompletableFuture.allOf(
                sendVideoViewEvent(viewEvent),
                sendUserActivityEvent(activityEvent)
        ).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✓ All batch events sent successfully");
            } else {
                log.error("✗ Some batch events failed", ex);
            }
        });
    }

    /**
     * Method chung để gửi message với callback
     */
    private CompletableFuture<SendResult<String, Object>> sendMessage(
            KafkaTemplate<String, Object> template,
            String topic,
            String key,
            Object event,
            String eventType) {

        CompletableFuture<SendResult<String, Object>> future = template.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("✓ {} event sent - topic: {}, partition: {}, offset: {}",
                        eventType,
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("✗ Failed to send {} event to topic: {}, key: {}",
                        eventType, topic, key, ex);
            }
        });

        return future;
    }

    /**
     * Alert khi payment fail
     */
    private void alertPaymentFailure(PaymentEvent event, Throwable ex) {
        // TODO: Implement alerting mechanism
        // - Gửi email cho admin
        // - Trigger PagerDuty/Slack alert
        // - Log vào monitoring system
        log.error("PAYMENT ALERT: Transaction {} failed to send to Kafka",
                event.getTransactionId());
    }

    /**
     * Kiểm tra health của Kafka producer
     */
    public boolean isKafkaAvailable() {
        try {
            // Send một test message
            standardTemplate.send(videoViewTopic, "health-check", "ping")
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return false;
        }
    }
}
