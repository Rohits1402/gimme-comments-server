package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.User;

public record AuthorResponse(
        String id,
        String name,
        @JsonProperty("profile_image") String profileImage
) {
    public static AuthorResponse from(User user) {
        if (user == null) {
            return new AuthorResponse(null, "Deleted user", null);
        }
        return new AuthorResponse(user.getId(), user.getName(), user.getProfileImage());
    }
}