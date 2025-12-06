package com.netfliz.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userAvatar;
    private String content;
    private Integer likeCount;
    private Boolean isLiked;
    private Boolean isEdited;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer replyCount;
    private List<CommentResponse> replies;
}
