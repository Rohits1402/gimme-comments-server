package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.Like;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LikeResponse(String id,
                           @JsonProperty("by_user") String userId,
                           @JsonProperty("on_comment") String commentId) {
    public static LikeResponse from(Like l) {
        return new LikeResponse(l.getId(), l.getUserId(), l.getCommentId());
    }
}
