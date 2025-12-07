package com.netfliz.worker.service;

import com.netfliz.worker.entity.analysis.Recommendation;
import com.netfliz.worker.entity.analysis.UserPreference;
import com.netfliz.worker.entity.analysis.Video;
import com.netfliz.worker.model.event.RecommendationEvent;
import com.netfliz.worker.repository.analysis.RecommendationRepository;
import com.netfliz.worker.repository.analysis.UserPreferenceRepository;
import com.netfliz.worker.repository.analysis.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final VideoRepository videoRepository;
    private final KafkaProducerService kafkaProducer;

    /**
     * Update user preferences dựa trên hành vi xem
     * Được gọi khi user xem video, like, hoặc tương tác
     */
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userId")
    public void updateUserPreferences(Long userId, Long videoId) {
        try {
            log.debug("Updating user preferences - userId: {}, videoId: {}", userId, videoId);

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

            // Lấy hoặc tạo mới user preference
            Optional<UserPreference> existingPref =
                    userPreferenceRepository.findByUserIdAndGenre(userId, video.getGenre());

            UserPreference preference;
            if (existingPref.isPresent()) {
                preference = existingPref.get();
                preference.setScore(preference.getScore() + 1.0);
                preference.setInteractionCount(preference.getInteractionCount() + 1);
                preference.setLastUpdated(LocalDateTime.now());
            } else {
                preference = UserPreference.builder()
                        .userId(userId)
                        .genre(video.getGenre())
                        .score(1.0)
                        .interactionCount(1)
                        .createdAt(LocalDateTime.now())
                        .lastUpdated(LocalDateTime.now())
                        .build();
            }

            userPreferenceRepository.save(preference);

            log.info("✓ User preferences updated - userId: {}, genre: {}, score: {}",
                    userId, video.getGenre(), preference.getScore());

            // Trigger recommendation generation asynchronously
            generatePersonalizedRecommendations(userId);

        } catch (Exception e) {
            log.error("Failed to update user preferences", e);
        }
    }

    /**
     * Generate personalized recommendations cho user
     * Sử dụng ML model hoặc collaborative filtering
     */
    @Transactional
    public void generatePersonalizedRecommendations(Long userId) {
        try {
            log.debug("Generating personalized recommendations for userId: {}", userId);

            // Lấy user preferences
            List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);

            if (preferences.isEmpty()) {
                log.debug("No preferences found for userId: {}", userId);
                return;
            }

            // Sort by score để lấy genre yêu thích nhất
            String favoriteGenre = preferences.stream()
                    .max((p1, p2) -> Double.compare(p1.getScore(), p2.getScore()))
                    .map(UserPreference::getGenre)
                    .orElse(null);

            if (favoriteGenre == null) {
                return;
            }

            // Tìm videos cùng genre chưa xem
            List<Video> recommendedVideos = videoRepository
                    .findTop10ByGenreOrderByViewCountDesc(favoriteGenre);

            // Tạo recommendation events
            for (Video video : recommendedVideos) {
                RecommendationEvent event = RecommendationEvent.builder()
                        .userId(userId)
                        .videoId(video.getId())
                        .recommendationType("PERSONALIZED")
                        .score(calculateRecommendationScore(userId, video))
                        .generatedAt(LocalDateTime.now())
                        .build();

                kafkaProducer.sendRecommendationEvent(event);
            }

            log.info("✓ Generated {} personalized recommendations for userId: {}",
                    recommendedVideos.size(), userId);

        } catch (Exception e) {
            log.error("Failed to generate personalized recommendations", e);
        }
    }

    /**
     * Generate similar movie recommendations
     * Được gọi khi user xem xong một video
     */
    @Transactional
    public void generateSimilarMovieRecommendations(Long userId, Long videoId) {
        try {
            log.debug("Generating similar movie recommendations - userId: {}, videoId: {}",
                    userId, videoId);

            Video watchedVideo = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

            // Tìm videos tương tự (cùng genre, director, cast)
            List<Video> similarVideos = videoRepository
                    .findSimilarVideos(watchedVideo.getGenre(),
                            watchedVideo.getDirector(),
                            videoId);

            // Tạo recommendation events
            for (Video video : similarVideos) {
                RecommendationEvent event = RecommendationEvent.builder()
                        .userId(userId)
                        .videoId(video.getId())
                        .recommendationType("SIMILAR")
                        .score(0.8)  // Fixed score cho similar videos
                        .generatedAt(LocalDateTime.now())
                        .build();

                kafkaProducer.sendRecommendationEvent(event);
            }

            log.info("✓ Generated {} similar movie recommendations for userId: {}",
                    similarVideos.size(), userId);

        } catch (Exception e) {
            log.error("Failed to generate similar movie recommendations", e);
        }
    }

    /**
     * Lưu recommendation vào database
     */
    @Transactional
    @CacheEvict(value = "recommendations", key = "#event.userId")
    public void saveRecommendation(RecommendationEvent event) {
        try {
            log.debug("Saving recommendation - userId: {}, videoId: {}, type: {}",
                    event.getUserId(), event.getVideoId(), event.getRecommendationType());

            // Check if recommendation already exists
            Optional<Recommendation> existing = recommendationRepository
                    .findByUserIdAndVideoIdAndType(
                            event.getUserId(),
                            event.getVideoId(),
                            event.getRecommendationType()
                    );

            Recommendation recommendation;
            if (existing.isPresent()) {
                recommendation = existing.get();
                recommendation.setScore(event.getScore());
                recommendation.setUpdatedAt(LocalDateTime.now());
            } else {
                recommendation = Recommendation.builder()
                        .userId(event.getUserId())
                        .videoId(event.getVideoId())
                        .type(event.getRecommendationType())
                        .score(event.getScore())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .viewed(false)
                        .clicked(false)
                        .build();
            }

            recommendationRepository.save(recommendation);

            log.debug("✓ Recommendation saved - userId: {}, videoId: {}",
                    event.getUserId(), event.getVideoId());

        } catch (Exception e) {
            log.error("Failed to save recommendation", e);
            throw e;
        }
    }

    /**
     * Lấy recommendations cho user từ cache
     */
    @Cacheable(value = "recommendations", key = "#userId")
    public List<Recommendation> getRecommendationsForUser(Long userId) {
        log.debug("Fetching recommendations from DB for userId: {}", userId);
        return recommendationRepository.findByUserIdAndViewedFalseOrderByScoreDesc(userId);
    }

    /**
     * Generate trending recommendations
     * Chạy theo schedule (cron job)
     */
    @Transactional
    public void generateTrendingRecommendations() {
        try {
            log.info("Generating trending recommendations");

            // Lấy top trending videos (view count cao trong 7 ngày)
            List<Video> trendingVideos = videoRepository
                    .findTrendingVideos(LocalDateTime.now().minusDays(7), 20);

            // Broadcast cho tất cả active users
            List<Long> activeUserIds = userPreferenceRepository.findActiveUserIds();

            int totalRecommendations = 0;
            for (Long userId : activeUserIds) {
                for (Video video : trendingVideos) {
                    RecommendationEvent event = RecommendationEvent.builder()
                            .userId(userId)
                            .videoId(video.getId())
                            .recommendationType("TRENDING")
                            .score(0.7)
                            .generatedAt(LocalDateTime.now())
                            .build();

                    kafkaProducer.sendRecommendationEvent(event);
                    totalRecommendations++;
                }
            }

            log.info("✓ Generated {} trending recommendations for {} users",
                    totalRecommendations, activeUserIds.size());

        } catch (Exception e) {
            log.error("Failed to generate trending recommendations", e);
        }
    }

    /**
     * Calculate recommendation score
     * Có thể sử dụng ML model hoặc rule-based scoring
     */
    private Double calculateRecommendationScore(Long userId, Video video) {
        try {
            // Simple scoring logic
            // TODO: Replace with ML model

            double score = 0.5;  // Base score

            // Bonus for popular videos
            if (video.getViewCount() > 10000) {
                score += 0.2;
            }

            // Bonus for recent videos
            if (video.getReleaseDate().isAfter(LocalDateTime.now().minusMonths(6))) {
                score += 0.1;
            }

            // Bonus for high rating
            if (video.getRating() != null && video.getRating() > 8.0) {
                score += 0.2;
            }

            return Math.min(score, 1.0);

        } catch (Exception e) {
            log.warn("Failed to calculate recommendation score", e);
            return 0.5;
        }
    }
}
