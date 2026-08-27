package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.dto.AuthorResponse;
import io.github.rohits1402.gimmecomments.dto.OverviewResponse;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.repository.CommentLikeRepository;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.LikeCount;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OverviewService {

    private static final int RECENT_LIMIT = 8;
    private static final int DAYS = 14;

    private final CommentRepository comments;
    private final CommentLikeRepository likes;
    private final WebsiteRepository websites;

    public OverviewService(CommentRepository comments,
                           CommentLikeRepository likes,
                           WebsiteRepository websites) {
        this.comments = comments;
        this.likes = likes;
        this.websites = websites;
    }

    @Transactional(readOnly = true)
    public OverviewResponse load(String userId) {
        UUID ownerId = toUuidOrNull(userId);
        if (ownerId == null) {
            return empty();
        }

        OverviewResponse.Totals totals = new OverviewResponse.Totals(
                comments.countForOwner(ownerId),
                likes.countForOwner(ownerId),
                comments.countDistinctAuthorsForOwner(ownerId),
                websites.countByOwnerId(ownerId));

        return new OverviewResponse(totals, daily(ownerId), recent(ownerId));
    }

    private static UUID toUuidOrNull(String id) {
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static OverviewResponse empty() {
        return new OverviewResponse(
                new OverviewResponse.Totals(0, 0, 0, 0), List.of(), List.of());
    }

    /**
     * Days are counted in UTC. Someone far from that meridian will occasionally see a
     * comment fall on the neighbouring bar, which is a fair trade for a fourteen-bar
     * strip against either asking the browser for its offset or storing one per account.
     */
    private List<OverviewResponse.DayCount> daily(UUID ownerId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate first = today.minusDays(DAYS - 1L);

        Map<LocalDate, Long> counted = comments
                .createdAtForOwnerSince(ownerId, first.atStartOfDay(ZoneOffset.UTC).toInstant())
                .stream()
                .collect(Collectors.groupingBy(
                        at -> at.atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.counting()));

        List<OverviewResponse.DayCount> out = new ArrayList<>(DAYS);
        for (int i = 0; i < DAYS; i++) {
            LocalDate day = first.plusDays(i);
            // Quiet days are sent as zeroes, so the chart keeps a fixed width and the
            // client never has to work out which days are missing.
            out.add(new OverviewResponse.DayCount(day, counted.getOrDefault(day, 0L)));
        }
        return out;
    }

    private List<OverviewResponse.RecentComment> recent(UUID ownerId) {
        List<Comment> found = comments.findRecentForOwner(ownerId, PageRequest.of(0, RECENT_LIMIT));
        if (found.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> likeCounts = likes
                .countByCommentIds(found.stream().map(Comment::getId).toList())
                .stream()
                .collect(Collectors.toMap(LikeCount::commentId, LikeCount::total));

        return found.stream()
                .map(c -> new OverviewResponse.RecentComment(
                        c.getId().toString(),
                        AuthorResponse.from(c.getAuthor()),
                        c.getCommentDescription(),
                        c.getCreatedAt(),
                        c.getParent() != null,
                        likeCounts.getOrDefault(c.getId(), 0L),
                        new OverviewResponse.RecentComment.Website(
                                c.getWebsite().getId().toString(),
                                c.getWebsite().getName())))
                .toList();
    }
}
