package io.github.rohits1402.gimmecomments.repository;

import java.util.UUID;

public record LikeCount(UUID commentId, long total) {
}