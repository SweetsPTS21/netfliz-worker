package com.netfliz.worker.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVideoRequest {
    @Size(min = 1, max = 500)
    private String title;

    @Size(min = 10, max = 5000)
    private String description;

    private String genre;

    private String director;

    private String cast;

    @Min(value = 1)
    private Integer duration;

    private String releaseDate;

    private String videoUrl;

    private String thumbnailUrl;

    private String trailerUrl;
}