package com.netfliz.worker.repository;

import com.netfliz.worker.entity.WatchProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchProgressRepository extends JpaRepository<WatchProgress, Long> {

    Optional<WatchProgress> findByUserIdAndVideoId(Long userId, Long videoId);

    List<WatchProgress> findByUserId(Long userId);

    List<WatchProgress> findByUserIdAndPercentCompleteLessThan(Long userId, Integer percentComplete);

    @Query("SELECT wp FROM WatchProgress wp WHERE wp.userId = :userId " +
            "AND wp.percentComplete < 95 AND wp.percentComplete > 5 " +
            "ORDER BY wp.lastUpdated DESC")
    List<WatchProgress> findContinueWatching(@Param("userId") Long userId, Pageable pageable);
}
