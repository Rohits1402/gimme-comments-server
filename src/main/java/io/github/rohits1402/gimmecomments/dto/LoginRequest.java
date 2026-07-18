package io.github.rohits1402.gimmecomments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Please provide email") @Email String email,
        @NotBlank(message = "Please provide password") String password
) {
}
