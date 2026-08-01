package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateWebsiteRequest(
        @JsonProperty("website_name")
        @NotBlank(message = "Please provide name") String websiteName,
        @JsonProperty("website_description") String websiteDescription,
        @JsonProperty("website_url")
        @NotBlank(message = "Please provide website URL") String websiteUrl,
        @JsonProperty("website_configuration") Map<String, Object> websiteConfiguration
) {
}
