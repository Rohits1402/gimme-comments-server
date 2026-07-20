package io.github.rohits1402.gimmecomments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OtpRequest(
        @NotBlank @Email String email,
        @NotBlank(message = "Please provide otp") String otp
) {
}