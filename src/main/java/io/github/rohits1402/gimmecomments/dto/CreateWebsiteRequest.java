package io.github.rohits1402.gimmecomments.dto;

import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateWebsiteRequest(
        @NotBlank(message = "Please provide name") String websiteName,
        String websiteDescription,
        @NotBlank(message = "Please provide website URL") String websiteUrl,
        Map<String, Object> websiteConfiguration) {
}