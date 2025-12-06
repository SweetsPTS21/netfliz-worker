package com.netfliz.worker.model.request;

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
public class ReportVideoRequest {
    @NotBlank(message = "Report reason is required")
    private String reason; // COPYRIGHT, INAPPROPRIATE, BROKEN, OTHER

    @Size(max = 1000)
    private String description;

    private String timestamp; // Optional: timestamp where issue occurs
}