package com.netfliz.worker.repository.analysis;

import com.netfliz.worker.entity.analysis.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    Optional<Watchlist> findByUserIdAndVideoId(Long userId, Long videoId);

    List<Watchlist> findByUserIdOrderByAddedAtDesc(Long userId);

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    void deleteByUserIdAndVideoId(Long userId, Long videoId);

    long countByUserId(Long userId);
}
