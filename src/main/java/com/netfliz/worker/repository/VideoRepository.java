package com.netfliz.worker.repository;

import com.netfliz.worker.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// ==================== VIDEO REPOSITORY ====================
@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findTop10ByGenreOrderByViewCountDesc(String genre);

    @Query("SELECT v FROM Video v WHERE v.genre = :genre AND v.director = :director AND v.id != :excludeId ORDER BY v.viewCount DESC")
    List<Video> findSimilarVideos(@Param("genre") String genre, @Param("director") String director, @Param("excludeId") Long excludeId);

    @Query("SELECT v FROM Video v WHERE v.lastViewedAt >= :since ORDER BY v.viewCount DESC LIMIT :limit")
    List<Video> findTrendingVideos(@Param("since") LocalDateTime since, int limit);

    // New search methods
    @Query("SELECT v FROM Video v WHERE " +
            "LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(v.director) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(v.cast) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Video> searchByQuery(@Param("query") String query, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE " +
            "(LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "v.genre = :genre")
    Page<Video> searchByQueryAndGenre(@Param("query") String query, @Param("genre") String genre, Pageable pageable);

    // Filter methods
    @Query("SELECT v FROM Video v WHERE " +
            "(:genre IS NULL OR v.genre = :genre) AND " +
            "(:director IS NULL OR v.director = :director) AND " +
            "(:releaseYear IS NULL OR YEAR(v.releaseDate) = :releaseYear) AND " +
            "(:minRating IS NULL OR v.rating >= :minRating)")
    Page<Video> filterVideos(
            @Param("genre") String genre,
            @Param("director") String director,
            @Param("releaseYear") Integer releaseYear,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    // Personalized recommendations based on user preferences
    @Query("SELECT v FROM Video v WHERE v.genre IN " +
            "(SELECT up.genre FROM UserPreference up WHERE up.userId = :userId ORDER BY up.score DESC) " +
            "AND v.id NOT IN (SELECT vh.videoId FROM ViewHistory vh WHERE vh.userId = :userId) " +
            "ORDER BY v.viewCount DESC, v.rating DESC")
    List<Video> findPersonalizedRecommendations(@Param("userId") Long userId, Pageable pageable);

    // New releases
    @Query("SELECT v FROM Video v WHERE v.releaseDate >= :since ORDER BY v.releaseDate DESC")
    List<Video> findNewReleases(@Param("since") LocalDateTime since, Pageable pageable);

    // Top rated
    @Query("SELECT v FROM Video v WHERE v.rating IS NOT NULL ORDER BY v.rating DESC, v.viewCount DESC")
    List<Video> findTopRated(Pageable pageable);

    // Most viewed
    @Query("SELECT v FROM Video v ORDER BY v.viewCount DESC")
    List<Video> findMostViewed(Pageable pageable);

    // Find by genre
}
