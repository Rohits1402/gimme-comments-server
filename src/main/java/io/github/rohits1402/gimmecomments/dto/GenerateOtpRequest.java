package io.github.rohits1402.gimmecomments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GenerateOtpRequest(
        @NotBlank @Email(message = "Please provide valid email") String email
) {}