package com.netfliz.worker.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProgressRequest {
    @NotNull(message = "Current position is required")
    @Min(value = 0, message = "Position must be positive")
    private Integer currentPosition; // seconds

    @NotNull(message = "Total duration is required")
    @Min(value = 1, message = "Duration must be positive")
    private Integer totalDuration; // seconds

    @NotNull(message = "Percent complete is required")
    @Min(value = 0)
    @Max(value = 100)
    private Integer percentComplete;
}
