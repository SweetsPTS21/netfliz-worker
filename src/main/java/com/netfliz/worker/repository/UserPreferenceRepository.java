package com.netfliz.worker.repository;

import com.netfliz.worker.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserIdAndGenre(Long userId, String genre);

    List<UserPreference> findByUserId(Long userId);

    @Query("SELECT DISTINCT up.userId FROM UserPreference up WHERE up.lastUpdated >= :since")
    List<Long> findActiveUserIds(@Param("since") LocalDateTime since);

    default List<Long> findActiveUserIds() {
        return findActiveUserIds(LocalDateTime.now().minusDays(30));
    }
}
