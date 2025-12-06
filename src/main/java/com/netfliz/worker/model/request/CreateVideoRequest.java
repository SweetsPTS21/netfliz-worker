package com.netfliz.worker.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVideoRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 500)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000)
    private String description;

    @NotBlank(message = "Genre is required")
    private String genre;

    @NotBlank(message = "Director is required")
    private String director;

    private String cast;

    @NotNull(message = "Duration is required")
    @Min(value = 1)
    private Integer duration; // minutes

    @NotNull(message = "Release date is required")
    private String releaseDate; // ISO format

    @NotBlank(message = "Video URL is required")
    private String videoUrl;

    @NotBlank(message = "Thumbnail URL is required")
    private String thumbnailUrl;

    private String trailerUrl;
}
