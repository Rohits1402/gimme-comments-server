package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateWebsiteRequest(
        @JsonProperty("website_name") String websiteName,
        @JsonProperty("website_description") String websiteDescription,
        @JsonProperty("website_configuration") Map<String, Object> websiteConfiguration
) {
}
