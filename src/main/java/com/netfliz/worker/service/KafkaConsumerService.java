package com.netfliz.worker.service;

import com.netfliz.worker.model.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final FileService fileService;
    private final MovieAssetService movieAssetService;

    private final VideoService videoService;
    private final RecommendationService recommendationService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${kafka.topics.update-movie-asset}",
            groupId = "update-movie-asset-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUpdateMovieAssetEvent(@Payload UpdateMovieAssetEvent event,
                                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                             @Header(KafkaHeaders.OFFSET) long offset,
                                             Acknowledgment acknowledgment) {

        try {
            log.info("Processing UpdateMovieAsset - objectId: {}, partition: {}, offset: {}",
                    event.getPayload().getObjectId(), partition, offset);

            var payload = event.getPayload();
            if (Objects.isNull(payload)) {
                log.error("✗ UpdateMovieAsset event is null");
                return;
            }

            var file = payload.getFile();
            if (Objects.isNull(file)) {
                log.error("✗ UpdateMovieAsset event file is null");
                return;
            }

            // save file
            var fileEntity = fileService.saveFile(file);

            // save asset
            movieAssetService.saveMovieAsset(payload, fileEntity.getId());

            // 4. Acknowledge message
            acknowledgment.acknowledge();

            log.debug("UpdateMovieAsset processed successfully - objectId: {}", event.getPayload().getObjectId());

        } catch (Exception e) {
            log.error("Error processing UpdateMovieAsset event - objectId: {}",
                    event.getPayload().getObjectId(), e);
            // Không acknowledge để retry
        }
    }

    /**
     * Consumer cho VIDEO VIEW events
     * - Cập nhật view count
     * - Lưu lịch sử xem
     * - Trigger recommendation
     */
    @KafkaListener(
            topics = "${kafka.topics.video-view}",
            groupId = "video-view-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVideoViewEvent(
            @Payload VideoViewEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("📺 Processing VideoView - userId: {}, videoId: {}, partition: {}, offset: {}",
                    event.getUserId(), event.getVideoId(), partition, offset);

            // 1. Cập nhật view count
            videoService.incrementViewCount(event.getVideoId());

            // 2. Lưu lịch sử xem
            videoService.saveViewHistory(event);

            // 3. Update user preferences cho recommendation
            recommendationService.updateUserPreferences(
                    event.getUserId(),
                    event.getVideoId()
            );

            // 4. Acknowledge message
            acknowledgment.acknowledge();

            log.debug("✓ VideoView processed successfully - userId: {}", event.getUserId());

        } catch (Exception e) {
            log.error("✗ Error processing VideoView event - userId: {}, videoId: {}",
                    event.getUserId(), event.getVideoId(), e);
            // Không acknowledge để retry
        }
    }

    /**
     * Consumer cho VIDEO PROGRESS events
     * - Lưu tiến độ xem
     * - Generate recommendations khi xem xong
     */
    @KafkaListener(
            topics = "${kafka.topics.video-progress}",
            groupId = "video-progress-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVideoProgressEvent(
            @Payload VideoProgressEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.debug("⏯️ Updating progress - userId: {}, videoId: {}, position: {}s/{}s ({}%)",
                    event.getUserId(),
                    event.getVideoId(),
                    event.getCurrentPosition(),
                    event.getTotalDuration(),
                    event.getPercentComplete());

            // Lưu tiến độ
            videoService.updateWatchProgress(
                    event.getUserId(),
                    event.getVideoId(),
                    event.getCurrentPosition(),
                    event.getPercentComplete()
            );

            // Nếu xem xong (>=95%), gợi ý phim tương tự
            if (event.getCompleted() != null && event.getCompleted()) {
                log.info("✓ User {} completed video {}, generating recommendations",
                        event.getUserId(), event.getVideoId());

                recommendationService.generateSimilarMovieRecommendations(
                        event.getUserId(),
                        event.getVideoId()
                );
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("✗ Error processing VideoProgress - userId: {}, videoId: {}",
                    event.getUserId(), event.getVideoId(), e);
            // Vẫn acknowledge vì progress không quá critical
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consumer cho USER ACTIVITY events
     * - Xử lý LIKE, COMMENT, SHARE, SEARCH, ADD_TO_WATCHLIST
     */
    @KafkaListener(
            topics = "${kafka.topics.user-activity}",
            groupId = "user-activity-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserActivityEvent(
            @Payload UserActivityEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.info("👤 Processing activity - userId: {}, type: {}, targetId: {}",
                    event.getUserId(), event.getActivityType(), event.getTargetId());

            switch (event.getActivityType()) {
                case "SEARCH":
                    analyticsService.trackSearch(event);
                    log.debug("Tracked search activity");
                    break;

                case "LIKE":
                    videoService.handleLike(event.getUserId(), event.getTargetId());
                    recommendationService.updateUserPreferences(
                            event.getUserId(),
                            event.getTargetId()
                    );
                    log.debug("Processed LIKE for videoId: {}", event.getTargetId());
                    break;

                case "COMMENT":
                    videoService.handleComment(event);
                    log.debug("Processed COMMENT");
                    break;

                case "SHARE":
                    videoService.incrementShareCount(event.getTargetId());
                    log.debug("Incremented share count for videoId: {}", event.getTargetId());
                    break;

                case "ADD_TO_WATCHLIST":
                    videoService.addToWatchlist(event.getUserId(), event.getTargetId());
                    log.debug("Added to watchlist - videoId: {}", event.getTargetId());
                    break;

                default:
                    log.warn("Unknown activity type: {}", event.getActivityType());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("✗ Error processing UserActivity - userId: {}, type: {}",
                    event.getUserId(), event.getActivityType(), e);
            acknowledgment.acknowledge(); // Acknowledge để không block queue
        }
    }

    /**
     * Consumer cho RECOMMENDATION events
     * - Lưu recommendations
     * - Gửi notification cho high-score recommendations
     */
    @KafkaListener(
            topics = "${kafka.topics.recommendation}",
            groupId = "recommendation-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRecommendationEvent(
            @Payload RecommendationEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.debug("💡 Processing recommendation - userId: {}, videoId: {}, type: {}, score: {}",
                    event.getUserId(), event.getVideoId(),
                    event.getRecommendationType(), event.getScore());

            // Lưu recommendation
            recommendationService.saveRecommendation(event);

            // Nếu score cao và là personalized, gửi notification
            if (event.getScore() != null && event.getScore() > 0.8
                    && "PERSONALIZED".equals(event.getRecommendationType())) {

                log.info("High-score recommendation detected, sending notification");
                notificationService.sendRecommendationNotification(event);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("✗ Error processing Recommendation - userId: {}, videoId: {}",
                    event.getUserId(), event.getVideoId(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consumer cho NOTIFICATION events
     * - Gửi notification qua multiple channels (push, email, in-app)
     * Sử dụng notificationListenerFactory với concurrency cao
     */
    @KafkaListener(
            topics = "${kafka.topics.notification}",
            groupId = "notification-group",
            containerFactory = "notificationListenerFactory"
    )
    public void consumeNotificationEvent(
            @Payload NotificationEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.info("🔔 Sending notification - userId: {}, type: {}, title: '{}'",
                    event.getUserId(), event.getType(), event.getTitle());

            // Gửi notification qua các kênh
            notificationService.sendMultiChannelNotification(event);

            acknowledgment.acknowledge();

            log.debug("✓ Notification sent successfully to userId: {}", event.getUserId());

        } catch (Exception e) {
            log.error("✗ Error sending notification - userId: {}, type: {}",
                    event.getUserId(), event.getType(), e);
            // Retry lại notification nếu fail
            // Hoặc acknowledge nếu không muốn block
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consumer cho PAYMENT events - CRITICAL
     * - Xử lý payment theo status
     * - Single thread để đảm bảo ordering
     * Sử dụng highPriorityListenerFactory
     */
    @KafkaListener(
            topics = "${kafka.topics.payment}",
            groupId = "payment-group",
            containerFactory = "highPriorityListenerFactory"
    )
    public void consumePaymentEvent(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.warn("💳 CRITICAL: Processing payment - transactionId: {}, userId: {}, " +
                            "status: {}, amount: {}, partition: {}, offset: {}",
                    event.getTransactionId(), event.getUserId(),
                    event.getStatus(), event.getAmount(), partition, offset);

            switch (event.getStatus()) {
                case "PENDING":
                    paymentService.processPendingPayment(event);
                    log.info("Payment pending processed - transactionId: {}",
                            event.getTransactionId());
                    break;

                case "COMPLETED":
                    // Xử lý payment thành công
                    paymentService.completePayment(event);

                    // Cập nhật subscription
                    paymentService.updateUserSubscription(
                            event.getUserId(),
                            event.getSubscriptionType()
                    );

                    // Gửi email confirmation
                    notificationService.sendPaymentConfirmation(event);

                    log.info("✓ Payment COMPLETED - transactionId: {}, userId: {}",
                            event.getTransactionId(), event.getUserId());
                    break;

                case "FAILED":
                    paymentService.handleFailedPayment(event);
                    notificationService.sendPaymentFailureNotification(event);

                    log.error("✗ Payment FAILED - transactionId: {}, userId: {}",
                            event.getTransactionId(), event.getUserId());
                    break;

                default:
                    log.warn("Unknown payment status: {}", event.getStatus());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("✗✗✗ CRITICAL ERROR processing payment - transactionId: {}",
                    event.getTransactionId(), e);
            // KHÔNG acknowledge để retry payment event
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    /**
     * Consumer cho ANALYTICS events - BATCH processing
     * - Xử lý batch để tăng throughput
     * - Có thể mất một số events (không critical)
     * Sử dụng analyticsListenerFactory
     */
    @KafkaListener(
            topics = "${kafka.topics.analytics}",
            groupId = "analytics-group",
            containerFactory = "analyticsListenerFactory"
    )
    public void consumeAnalyticsEventBatch(
            @Payload List<AnalyticsEvent> events,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            Acknowledgment acknowledgment) {

        try {
            log.info("📊 Processing analytics batch - size: {}, partitions: {}, offsets: {}",
                    events.size(), partitions, offsets);

            // Xử lý batch
            analyticsService.processBatchEvents(events);

            acknowledgment.acknowledge();

            log.debug("✓ Analytics batch processed - {} events", events.size());

        } catch (Exception e) {
            log.warn("⚠️ Error processing analytics batch (non-critical): {}",
                    e.getMessage());
            // Vẫn acknowledge vì analytics không critical
            acknowledgment.acknowledge();
        }
    }

    /**
     * Alternative: Single analytics consumer (không dùng batch)
     */
    @KafkaListener(
            topics = "${kafka.topics.analytics}",
            groupId = "analytics-single-group",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "false"  // Disabled by default
    )
    public void consumeAnalyticsEventSingle(
            @Payload AnalyticsEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.debug("📊 Processing analytics - type: {}, userId: {}",
                    event.getEventType(), event.getUserId());

            analyticsService.processEvent(event);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.warn("⚠️ Analytics event failed (non-critical): {}", e.getMessage());
            acknowledgment.acknowledge();
        }
    }
}
