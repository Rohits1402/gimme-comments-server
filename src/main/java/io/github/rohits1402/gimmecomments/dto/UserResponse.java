package io.github.rohits1402.gimmecomments.dto;

import io.github.rohits1402.gimmecomments.model.Gender;
import io.github.rohits1402.gimmecomments.model.User;

public record UserResponse(
        String id,
        String name,
        String email,
        Gender gender,
        String profileImage,
        boolean emailVerified
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getGender(),
                user.getProfileImage(),
                user.isEmailVerified()
        );
    }
}