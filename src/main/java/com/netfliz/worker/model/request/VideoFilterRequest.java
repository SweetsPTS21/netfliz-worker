package com.netfliz.worker.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFilterRequest {
    private String genre;

    private String director;

    private Integer releaseYear;

    @Min(value = 0)
    @Max(value = 10)
    private Double minRating;

    @Min(value = 0)
    private Integer page = 0;

    @Min(value = 1)
    @Max(value = 100)
    private Integer size = 20;

    private String sortBy = "view_count"; // view_count, rating, release_date, title

    private String sortOrder = "DESC"; // ASC, DESC
}
