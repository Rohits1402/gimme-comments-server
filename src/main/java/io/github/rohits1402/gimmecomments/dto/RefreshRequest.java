package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @JsonProperty("refresh_token") @NotBlank(message = "Please provide refresh token") String refreshToken
) {
}