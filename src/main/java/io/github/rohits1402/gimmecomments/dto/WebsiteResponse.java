package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.model.jpa.Website;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebsiteResponse(
        String id,
        @JsonProperty("by_user") String userId,
        @JsonProperty("website_name") String websiteName,
        @JsonProperty("website_description") String websiteDescription,
        @JsonProperty("website_url") String websiteUrl,
        @JsonProperty("website_configuration") Map<String, Object> websiteConfiguration,
        @JsonProperty("created_at") Instant createdAt
) {
    public static WebsiteResponse from(Website w) {
        return new WebsiteResponse(
                w.getId().toString(),
                w.getOwner().getId().toString(),
                w.getName(),
                w.getDescription(),
                w.getUrl(),
                w.getWebsiteConfiguration(),
                w.getCreatedAt()
        );
    }
}
