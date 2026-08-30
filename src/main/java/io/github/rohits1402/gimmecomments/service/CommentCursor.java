package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Where a page of threads stopped — the sort key of the last root comment sent.
 * <p>
 * It travels as one opaque string on purpose. A caller who cannot read it cannot
 * depend on it, so the sort key can change later without breaking anyone. The
 * moment a cursor looks like a timestamp, somebody builds their own.
 */
record CommentCursor(Instant createdAt, UUID id) {

    private static final String SEPARATOR = "|";

    String encode() {
        String raw = createdAt + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static CommentCursor decode(String encoded) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int split = raw.indexOf(SEPARATOR);
            return new CommentCursor(Instant.parse(raw.substring(0, split)),
                    UUID.fromString(raw.substring(split + 1)));
        } catch (RuntimeException e) {
            // Anything malformed is the caller's problem, not a 500.
            throw new BadRequestException("Invalid cursor");
        }
    }
}