package com.netfliz.worker.repository.analysis;

import com.netfliz.worker.entity.analysis.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByUserIdAndVideoIdAndType(Long userId, Long videoId, String type);

    List<Recommendation> findByUserIdAndViewedFalseOrderByScoreDesc(Long userId);

    List<Recommendation> findByUserId(Long userId);
}