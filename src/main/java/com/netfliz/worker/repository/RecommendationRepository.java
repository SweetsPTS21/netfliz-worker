package com.netfliz.worker.repository;

import com.netfliz.worker.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByUserIdAndVideoIdAndType(Long userId, Long videoId, String type);

    List<Recommendation> findByUserIdAndViewedFalseOrderByScoreDesc(Long userId);

    List<Recommendation> findByUserId(Long userId);
}