package io.github.rohits1402.gimmecomments.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateWebsiteRequest(String websiteName, String websiteDescription,
                                   Map<String, Object> websiteConfiguration) {
}
