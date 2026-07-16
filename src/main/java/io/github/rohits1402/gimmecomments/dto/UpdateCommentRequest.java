package io.github.rohits1402.gimmecomments.dto;

import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateCommentRequest(
        @NotBlank(message = "Please provide comment description") String commentDescription
) {
}