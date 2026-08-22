package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.Gender;
import io.github.rohits1402.gimmecomments.model.jpa.User;


public record UserResponse(
        String id,
        String name,
        String email,
        Gender gender,
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("email_verified") boolean emailVerified
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getGender(),
                user.getProfileImage(),
                user.isEmailVerified()
        );
    }
}
