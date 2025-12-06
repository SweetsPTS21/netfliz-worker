package com.netfliz.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailResponse {
    private VideoResponse video;
    private List<VideoResponse> similarVideos;
    private List<CommentResponse> topComments;
    private VideoStatsResponse stats;
    private Boolean canWatch; // User có quyền xem không (subscription)
}
