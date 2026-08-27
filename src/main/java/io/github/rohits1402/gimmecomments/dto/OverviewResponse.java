package io.github.rohits1402.gimmecomments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the dashboard's front page needs, in one response.
 * <p>
 * This is shaped by the screen rather than by a table, which is unusual for this API
 * and deliberate. The alternative was the browser fetching every website, then every
 * website's comments, in order to work out three numbers and a short list. Four
 * aggregate queries on the server beat that at any size.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OverviewResponse(
        Totals totals,
        List<DayCount> daily,
        List<RecentComment> recent
) {
    public record Totals(long comments, long likes, long people, long websites) {
    }

    /** One bar of the activity strip. Quiet days are present, carrying a zero. */
    public record DayCount(LocalDate day, long count) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecentComment(
            String id,
            @JsonProperty("by_user") AuthorResponse author,
            @JsonProperty("comment_description") String commentDescription,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("is_reply") boolean isReply,
            @JsonProperty("liked_by") long likedBy,
            Website website
    ) {
        /** Only what the row needs in order to label itself and link. */
        public record Website(String id, @JsonProperty("website_name") String websiteName) {
        }
    }
}
