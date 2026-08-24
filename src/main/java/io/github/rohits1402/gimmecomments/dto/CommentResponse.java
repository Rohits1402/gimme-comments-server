package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.User;
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
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("liked_by") @JsonInclude(JsonInclude.Include.NON_NULL) Long likedBy,
        @JsonProperty("i_liked") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean iLiked
) {
    /** Single comment — create and update, where the old API sent no like data. */
    public static CommentResponse from(Comment c, User author) {
        return from(c, author, null, null);
    }

    /** Comment list — carries the like count and whether the caller liked it. */
    public static CommentResponse from(Comment c, User author, Long likedBy, Boolean iLiked) {
        return new CommentResponse(
                c.getId().toString(),
                AuthorResponse.from(author),
                c.getWebsite().getId().toString(),
                c.getParent() == null ? null : c.getParent().getId().toString(),
                c.getCommentDescription(),
                c.getCreatedAt(),
                likedBy,
                iLiked);
    }
}