package com.netfliz.worker.repository.analysis;

import com.netfliz.worker.entity.analysis.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndVideoId(Long userId, Long videoId);

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    void deleteByUserIdAndVideoId(Long userId, Long videoId);

    long countByVideoId(Long videoId);

    List<Like> findByUserId(Long userId);
}
