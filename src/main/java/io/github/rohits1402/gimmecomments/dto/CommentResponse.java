package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.Comment;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CommentResponse(
        String id,
        @JsonProperty("by_user") String userId,
        @JsonProperty("on_website") String websiteId,
        @JsonProperty("comment_parent") String parentCommentId,
        String commentDescription,
        Instant createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getUserId(), c.getWebsiteId(),
                c.getParentCommentId(), c.getCommentDescription(), c.getCreatedAt());
    }
}