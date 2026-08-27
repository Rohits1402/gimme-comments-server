package io.github.rohits1402.gimmecomments.repository;

import java.util.UUID;

/** How many comments one website has. Built by a grouped count, never by counting in a loop. */
public record CommentCount(UUID websiteId, long total) {
}
