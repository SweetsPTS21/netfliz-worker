package com.netfliz.worker.service;

import com.netfliz.worker.entity.analysis.Like;
import com.netfliz.worker.entity.analysis.Video;
import com.netfliz.worker.repository.analysis.LikeRepository;
import com.netfliz.worker.repository.analysis.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final VideoRepository videoRepository;

    /**
     * Like video
     */
    @Transactional
    public void likeVideo(Long userId, Long videoId) {
        try {
            log.info("Liking video - userId: {}, videoId: {}", userId, videoId);

            // Check if already liked
            if (likeRepository.existsByUserIdAndVideoId(userId, videoId)) {
                log.warn("Video already liked - userId: {}, videoId: {}", userId, videoId);
                return;
            }

            // Create like
            Like like = Like.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .createdAt(LocalDateTime.now())
                    .build();

            likeRepository.save(like);

            // Increment video like count
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            video.setLikeCount(video.getLikeCount() + 1);
            videoRepository.save(video);

            log.info("✓ Video liked - videoId: {}, new count: {}",
                    videoId, video.getLikeCount());

        } catch (Exception e) {
            log.error("Failed to like video", e);
            throw e;
        }
    }

    /**
     * Unlike video
     */
    @Transactional
    public void unlikeVideo(Long userId, Long videoId) {
        try {
            log.info("Unliking video - userId: {}, videoId: {}", userId, videoId);

            Like like = likeRepository.findByUserIdAndVideoId(userId, videoId)
                    .orElseThrow(() -> new RuntimeException("Video not liked"));

            likeRepository.delete(like);

            // Decrement video like count
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            video.setLikeCount(Math.max(0, video.getLikeCount() - 1));
            videoRepository.save(video);

            log.info("✓ Video unliked - videoId: {}, new count: {}",
                    videoId, video.getLikeCount());

        } catch (Exception e) {
            log.error("Failed to unlike video", e);
            throw e;
        }
    }

    /**
     * Check if user liked video
     */
    public boolean isVideoLiked(Long userId, Long videoId) {
        return likeRepository.existsByUserIdAndVideoId(userId, videoId);
    }

    /**
     * Get user's liked videos
     */
    public List<Long> getUserLikedVideoIds(Long userId) {
        return likeRepository.findByUserId(userId)
                .stream()
                .map(Like::getVideoId)
                .collect(Collectors.toList());
    }

    /**
     * Get video like count
     */
    public long getVideoLikeCount(Long videoId) {
        return likeRepository.countByVideoId(videoId);
    }

    /**
     * Get user's total likes count
     */
    public long getUserLikeCount(Long userId) {
        return likeRepository.countByVideoId(userId);
    }
}
