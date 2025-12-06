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
public class SearchResponse {
    private List<VideoResponse> videos;
    private Integer totalResults;
    private Integer currentPage;
    private Integer totalPages;
    private String query;
    private Long searchTime; // milliseconds
}