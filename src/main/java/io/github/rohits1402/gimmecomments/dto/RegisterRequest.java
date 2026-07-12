package io.github.rohits1402.gimmecomments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Please provide name")
        String name,

        @NotBlank(message = "Please provide email")
        @Email(message = "Please provide valid email")
        String email,

        @NotBlank(message = "Please provide password")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
