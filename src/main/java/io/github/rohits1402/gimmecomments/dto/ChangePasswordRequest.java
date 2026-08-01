package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChangePasswordRequest(
        @NotBlank @Email String email,
        @NotBlank(message = "Please provide otp") String otp,
        @JsonProperty("new_password")
        @NotBlank(message = "Please provide new password") String newPassword
) {
}
