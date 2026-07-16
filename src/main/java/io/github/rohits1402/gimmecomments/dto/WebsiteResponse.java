package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.Website;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebsiteResponse(String id,
                              @JsonProperty("by_user") String userId,
                              String websiteName, String websiteDescription,
                              String websiteUrl, Map<String, Object> websiteConfiguration, Instant createdAt) {
    public static WebsiteResponse from(Website w) {
        return new WebsiteResponse(w.getId(), w.getUserId(), w.getWebsiteName(), w.getWebsiteDescription(), w.getWebsiteUrl(), w.getWebsiteConfiguration(), w.getCreatedAt());
    }
}
