package com.netfliz.worker.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSearchRequest {
    @NotBlank(message = "Search query is required")
    @Size(min = 1, max = 200, message = "Query must be between 1 and 200 characters")
    private String query;

    private String genre;

    private Integer minRating;

    private Integer minYear;

    private Integer maxYear;

    @Min(value = 0)
    private Integer page = 0;

    @Min(value = 1)
    @Max(value = 100)
    private Integer size = 20;

    private String sortBy = "relevance"; // relevance, rating, views, date
}
