package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.UnauthenticatedException;
import io.github.rohits1402.gimmecomments.model.RefreshToken;
import io.github.rohits1402.gimmecomments.repository.RefreshTokenRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The revocable half of signing in. Access tokens are JWTs and are verified by
 * arithmetic alone, so nothing can cancel one; they are kept short for that reason.
 * Refresh tokens are the opposite - opaque, looked up in the database on every use,
 * and therefore cancellable the moment their row says so.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final Duration lifetime;

    private final TransactionTemplate inItsOwnTransaction;

    public RefreshTokenService(RefreshTokenRepository tokens,
                               UserRepository users,
                               PlatformTransactionManager transactionManager,
                               @Value("${app.refresh.expiration-ms}") long expirationMs) {
        this.tokens = tokens;
        this.users = users;
        this.lifetime = Duration.ofMillis(expirationMs);
        this.inItsOwnTransaction = new TransactionTemplate(transactionManager);
        this.inItsOwnTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * A brand-new session, in a family of its own.
     */
    @Transactional
    public String issue(UUID userId) {
        tokens.deleteByExpiresAtBefore(Instant.now());

        String raw = newToken();

        RefreshToken token = new RefreshToken();
        token.setUser(users.getReferenceById(userId));
        token.setFamilyId(UUID.randomUUID());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(lifetime));
        tokens.save(token);

        return raw;
    }

    /**
     * Who a refresh token belongs to, provided it is still good. Unknown, revoked and
     * expired all fail identically: telling a caller which one it was would tell them
     * whether the token had ever been real.
     */
    /**
     * How long after a rotation a repeat of the same token is read as a second tab.
     */
    private static final Duration RACE_GRACE = Duration.ofSeconds(30);

    /**
     * Who the session belongs to, and the token that replaces the one just used.
     */
    public record Rotation(UUID userId, String refreshToken) {
    }

    /**
     * Exchange a refresh token for its successor.
     * <p>
     * Unknown, revoked and expired all fail the same way, so a caller cannot learn
     * whether a token was ever real.
     */
    @Transactional
    public Rotation rotate(String raw) {
        RefreshToken presented = tokens.findByTokenHash(hash(raw))
                .filter(RefreshToken::isUsable)
                .orElseThrow(() -> new UnauthenticatedException("Invalid refresh token"));

        Instant now = Instant.now();

        if (presented.getRotatedAt() != null
                && presented.getRotatedAt().isBefore(now.minus(RACE_GRACE))) {
            // Committed separately, because the very next line throws and the default
            // rollback rule would undo this. Noticing a stolen session and then losing
            // the record of it is the same as never noticing.
            inItsOwnTransaction.executeWithoutResult(status ->
                    tokens.revokeFamily(presented.getFamilyId(), now));
            throw new UnauthenticatedException("Invalid refresh token");
        }

        // Anchored to the first exchange, not refreshed on every repeat, so the window
        // cannot be stretched by presenting the same token over and over.
        if (presented.getRotatedAt() == null) {
            presented.setRotatedAt(now);
        }

        UUID userId = presented.getUser().getId();
        return new Rotation(userId, issueInFamily(userId, presented.getFamilyId()));
    }

    /**
     * Ends the session a refresh token belongs to, and every token in it.
     */
    @Transactional
    public void revokeSession(String raw) {
        tokens.findByTokenHash(hash(raw))
                .ifPresent(t -> tokens.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    private String issueInFamily(UUID userId, UUID familyId) {
        String raw = newToken();

        RefreshToken token = new RefreshToken();
        token.setUser(users.getReferenceById(userId));
        token.setFamilyId(familyId);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(lifetime));
        tokens.save(token);

        return raw;
    }

    /**
     * 256 bits of randomness, URL-safe because it travels in JSON.
     */
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, not bcrypt. bcrypt is slow on purpose, to make guessing a human-chosen
     * password expensive. Nobody is guessing 256 random bits, so slowness would buy
     * nothing and cost a slow hash on every refresh.
     */
    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Every JVM is required to provide SHA-256.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}