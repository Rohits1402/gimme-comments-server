package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.jpa.User;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CommentResponse(
        String id,
        @JsonProperty("by_user") AuthorResponse author,
        @JsonProperty("on_website") String websiteId,
        @JsonProperty("comment_parent") String parentCommentId,
        @JsonProperty("comment_description") String commentDescription,
        @JsonProperty("created_at") Instant createdAt
) {
    public static CommentResponse from(Comment c, User author) {
        return new CommentResponse(
                c.getId(),
                AuthorResponse.from(author),
                c.getWebsiteId(),
                c.getParentCommentId(),
                c.getCommentDescription(),
                c.getCreatedAt());
    }
}
