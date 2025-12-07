package com.netfliz.worker.service;

import com.netfliz.worker.entity.analysis.Notification;
import com.netfliz.worker.entity.analysis.UserNotificationPreference;
import com.netfliz.worker.model.event.NotificationEvent;
import com.netfliz.worker.model.event.PaymentEvent;
import com.netfliz.worker.model.event.RecommendationEvent;
import com.netfliz.worker.repository.analysis.NotificationRepository;
import com.netfliz.worker.repository.analysis.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    /**
     * Gửi notification qua nhiều kênh (push, email, in-app)
     */
    @Transactional
    public void sendMultiChannelNotification(NotificationEvent event) {
        try {
            log.info("Sending multi-channel notification - userId: {}, type: {}",
                    event.getUserId(), event.getType());

            // Lấy user preferences
            UserNotificationPreference preference = preferenceRepository
                    .findByUserId(event.getUserId())
                    .orElse(getDefaultPreference(event.getUserId()));

            // Lưu notification vào database
            Notification notification = saveNotification(event);

            // Gửi qua các kênh dựa trên user preference
            if (preference.isPushEnabled()) {
                sendPushNotification(event);
            }

            if (preference.isEmailEnabled()) {
                sendEmailNotification(event);
            }

            // In-app notification luôn được lưu
            // Client sẽ poll hoặc subscribe qua WebSocket

            // Mark as sent
            notification.setSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("✓ Multi-channel notification sent - userId: {}, type: {}",
                    event.getUserId(), event.getType());

        } catch (Exception e) {
            log.error("Failed to send multi-channel notification", e);
            throw e;
        }
    }

    /**
     * Gửi push notification
     * Sử dụng Firebase Cloud Messaging (FCM) hoặc APNs
     */
    private void sendPushNotification(NotificationEvent event) {
        try {
            log.debug("Sending push notification - userId: {}", event.getUserId());

            // TODO: Integrate with FCM/APNs
            // Example FCM payload
            Map<String, Object> fcmPayload = new HashMap<>();
            fcmPayload.put("to", getUserDeviceToken(event.getUserId()));
            fcmPayload.put("notification", Map.of(
                    "title", event.getTitle(),
                    "body", event.getMessage(),
                    "click_action", event.getDeepLink()
            ));

            // Send to FCM
            // restTemplate.postForEntity(FCM_URL, fcmPayload, String.class);

            log.debug("✓ Push notification sent - userId: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to send push notification", e);
        }
    }

    /**
     * Gửi email notification
     */
    private void sendEmailNotification(NotificationEvent event) {
        try {
            log.debug("Sending email notification - userId: {}", event.getUserId());

            String userEmail = getUserEmail(event.getUserId());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userEmail);
            message.setSubject(event.getTitle());
            message.setText(buildEmailBody(event));
            message.setFrom("noreply@moviestreaming.com");

            mailSender.send(message);

            log.debug("✓ Email sent - userId: {}, email: {}",
                    event.getUserId(), userEmail);

        } catch (Exception e) {
            log.error("Failed to send email notification", e);
        }
    }

    /**
     * Gửi notification cho recommendation
     */
    public void sendRecommendationNotification(RecommendationEvent event) {
        try {
            log.debug("Sending recommendation notification - userId: {}, videoId: {}",
                    event.getUserId(), event.getVideoId());

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(event.getUserId())
                    .type("RECOMMENDATION")
                    .title("New Movie Recommendation!")
                    .message("We found a movie you might love. Check it out!")
                    .deepLink("/videos/" + event.getVideoId())
                    .createdAt(LocalDateTime.now())
                    .sent(false)
                    .build();

            sendMultiChannelNotification(notificationEvent);

        } catch (Exception e) {
            log.error("Failed to send recommendation notification", e);
        }
    }

    /**
     * Gửi payment confirmation
     */
    public void sendPaymentConfirmation(PaymentEvent event) {
        try {
            log.info("Sending payment confirmation - userId: {}, transactionId: {}",
                    event.getUserId(), event.getTransactionId());

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(event.getUserId())
                    .type("PAYMENT")
                    .title("Payment Successful")
                    .message(String.format(
                            "Your payment of %s %s for %s subscription has been confirmed. Transaction ID: %s",
                            event.getAmount(),
                            event.getCurrency(),
                            event.getSubscriptionType(),
                            event.getTransactionId()
                    ))
                    .deepLink("/account/subscription")
                    .createdAt(LocalDateTime.now())
                    .sent(false)
                    .build();

            sendMultiChannelNotification(notificationEvent);

            log.info("✓ Payment confirmation sent - userId: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to send payment confirmation", e);
        }
    }

    /**
     * Gửi payment failure notification
     */
    public void sendPaymentFailureNotification(PaymentEvent event) {
        try {
            log.info("Sending payment failure notification - userId: {}, transactionId: {}",
                    event.getUserId(), event.getTransactionId());

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(event.getUserId())
                    .type("PAYMENT")
                    .title("Payment Failed")
                    .message(String.format(
                            "Your payment of %s %s failed. Please update your payment method.",
                            event.getAmount(),
                            event.getCurrency()
                    ))
                    .deepLink("/account/payment-methods")
                    .createdAt(LocalDateTime.now())
                    .sent(false)
                    .build();

            sendMultiChannelNotification(notificationEvent);

            log.info("✓ Payment failure notification sent - userId: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to send payment failure notification", e);
        }
    }

    /**
     * Gửi new episode notification
     */
    public void sendNewEpisodeNotification(Long userId, Long seriesId, String episodeTitle) {
        try {
            log.info("Sending new episode notification - userId: {}, seriesId: {}",
                    userId, seriesId);

            NotificationEvent event = NotificationEvent.builder()
                    .userId(userId)
                    .type("NEW_EPISODE")
                    .title("New Episode Available!")
                    .message("A new episode of your favorite series is now available: " + episodeTitle)
                    .deepLink("/series/" + seriesId)
                    .createdAt(LocalDateTime.now())
                    .sent(false)
                    .build();

            sendMultiChannelNotification(event);

        } catch (Exception e) {
            log.error("Failed to send new episode notification", e);
        }
    }

    /**
     * Lưu notification vào database
     */
    @Transactional
    private Notification saveNotification(NotificationEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .type(event.getType())
                .title(event.getTitle())
                .message(event.getMessage())
                .deepLink(event.getDeepLink())
                .sent(false)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Build email body
     */
    private String buildEmailBody(NotificationEvent event) {
        StringBuilder body = new StringBuilder();
        body.append("Hello,\n\n");
        body.append(event.getMessage());
        body.append("\n\n");

        if (event.getDeepLink() != null) {
            body.append("Click here to view: https://moviestreaming.com")
                    .append(event.getDeepLink());
            body.append("\n\n");
        }

        body.append("Best regards,\n");
        body.append("Movie Streaming Team");

        return body.toString();
    }

    /**
     * Get user device token for push notifications
     */
    private String getUserDeviceToken(Long userId) {
        // TODO: Implement device token retrieval from database
        return "device_token_" + userId;
    }

    /**
     * Get user email
     */
    private String getUserEmail(Long userId) {
        // TODO: Implement user email retrieval from database
        return "user" + userId + "@example.com";
    }

    /**
     * Get default notification preference
     */
    private UserNotificationPreference getDefaultPreference(Long userId) {
        return UserNotificationPreference.builder()
                .userId(userId)
                .pushEnabled(true)
                .emailEnabled(true)
                .smsEnabled(false)
                .build();
    }
}
