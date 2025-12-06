package com.netfliz.worker.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ==================== WATCH REQUEST ====================
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchRequest {
    @NotNull(message = "Device type is required")
    private String deviceType; // WEB, MOBILE, SMART_TV, TABLET

    private String ipAddress;

    @NotNull(message = "Quality is required")
    private String quality; // 480p, 720p, 1080p, 4K

    private String userAgent;
}
