package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.TestDatabase;
import io.github.rohits1402.gimmecomments.exception.ForbiddenException;
import io.github.rohits1402.gimmecomments.exception.UnauthenticatedException;
import io.github.rohits1402.gimmecomments.model.RefreshToken;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.repository.RefreshTokenRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Access tokens are verified by arithmetic and cannot be cancelled, so they are short.
 * Refresh tokens are looked up on every use, so they can be. These tests are about
 * that second property: every way a session should stop working, it does.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
@Transactional
class RefreshTokenTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository users;
    @Autowired
    private RefreshTokenRepository refreshTokens;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    private User user;
    private String email;

    @BeforeEach
    void setUp() {
        email = "session-" + UUID.randomUUID() + "@example.test";

        User u = new User();
        u.setName("Session");
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setEmailVerified(true);
        user = users.save(u);
    }

    private List<RefreshToken> sessionsOf(User owner) {
        return refreshTokens.findAll().stream()
                .filter(t -> t.getUser().getId().equals(owner.getId()))
                .toList();
    }

    @Test
    void theDatabaseNeverHoldsTheTokenItself() {
        AuthTokens signedIn = userService.login(email, PASSWORD);

        List<RefreshToken> rows = sessionsOf(user);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getTokenHash())
                .as("a leaked table of hashes must not be a table of working logins")
                .isNotEqualTo(signedIn.refreshToken())
                .matches("[0-9a-f]{64}");
        assertThat(refreshTokens.findByTokenHash(signedIn.refreshToken())).isEmpty();
    }

    @Test
    void aValidRefreshTokenBuysAnAccessTokenForTheSameUser() {
        AuthTokens signedIn = userService.login(email, PASSWORD);

        AuthTokens refreshed = userService.refresh(signedIn.refreshToken());

        assertThat(jwtService.extractUserId(refreshed.accessToken()))
                .isEqualTo(user.getId().toString());
    }

    @Test
    void aTokenThatWasNeverIssuedIsRefused() {
        assertThatThrownBy(() -> userService.refresh("not-a-token-we-ever-issued"))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void aRevokedTokenIsRefused() {
        AuthTokens signedIn = userService.login(email, PASSWORD);

        sessionsOf(user).forEach(t -> t.setRevokedAt(Instant.now()));
        refreshTokens.flush();

        assertThatThrownBy(() -> userService.refresh(signedIn.refreshToken()))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void anExpiredTokenIsRefused() {
        AuthTokens signedIn = userService.login(email, PASSWORD);

        sessionsOf(user).forEach(t -> t.setExpiresAt(Instant.now().minusSeconds(1)));
        refreshTokens.flush();

        assertThatThrownBy(() -> userService.refresh(signedIn.refreshToken()))
                .isInstanceOf(UnauthenticatedException.class);
    }

    /**
     * Before today this could not be true: a deactivated account kept working until its
     * thirty-day token expired. Now it stops at the next refresh.
     */
    @Test
    void aDeactivatedAccountCannotRefresh() {
        AuthTokens signedIn = userService.login(email, PASSWORD);

        user.setAccountActive(false);
        users.saveAndFlush(user);

        assertThatThrownBy(() -> userService.refresh(signedIn.refreshToken()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deletingTheAccountEndsItsSessions() {
        AuthTokens signedIn = userService.login(email, PASSWORD);

        // Signing in and deleting an account are separate requests, each with its own
        // persistence context. Without this, the refresh token that login just saved is
        // still held by Hibernate in the same session as the user, and Hibernate refuses
        // to delete an entity that another managed entity still points at - before it
        // ever sends SQL. A real request could never be in that state.
        entityManager.flush();
        entityManager.clear();

        users.deleteById(user.getId());
        // DELETE FROM users, and PostgreSQL cascades refresh_tokens itself - behind
        // Hibernate's back. Clearing again makes the next read ask the database.
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> userService.refresh(signedIn.refreshToken()))
                .isInstanceOf(UnauthenticatedException.class);
    }
}