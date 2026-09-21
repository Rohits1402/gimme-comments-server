package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.TestDatabase;
import io.github.rohits1402.gimmecomments.exception.UnauthenticatedException;
import io.github.rohits1402.gimmecomments.model.RefreshToken;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.repository.RefreshTokenRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Refresh tokens are single-use. Presenting one twice means two parties held it, and
 * since there is no way to tell which is the owner, every session from that sign-in
 * ends.
 * <p>
 * NOT @Transactional, unlike the other refresh-token tests. Revoking a family happens
 * in its own transaction so an exception cannot undo it, and a separate transaction
 * cannot see rows the test has not committed. So this one commits and tidies up.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
class RefreshTokenRotationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository users;
    @Autowired
    private RefreshTokenRepository refreshTokens;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbc;

    private User user;
    private String email;

    @BeforeEach
    void setUp() {
        email = "rotation-" + UUID.randomUUID() + "@example.test";

        User u = new User();
        u.setName("Rotation");
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setEmailVerified(true);
        user = users.save(u);
    }

    @AfterEach
    void tearDown() {
        users.deleteById(user.getId());   // sessions go with it, by cascade
    }

    private List<RefreshToken> sessions() {
        return refreshTokens.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .toList();
    }

    /**
     * Moves the rotation far enough into the past that the grace window has passed.
     */
    private void pretendTheRotationWasLongAgo() {
        jdbc.update("UPDATE refresh_tokens SET rotated_at = rotated_at - interval '2 minutes' "
                + "WHERE rotated_at IS NOT NULL AND user_id = ?", user.getId());
    }

    @Test
    void rotatingIssuesANewTokenInTheSameFamily() {
        AuthTokens first = userService.login(email, PASSWORD);

        AuthTokens second = userService.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(sessions()).hasSize(2);
        assertThat(sessions().stream().map(RefreshToken::getFamilyId).distinct().toList())
                .as("a successor belongs to the sign-in it came from")
                .hasSize(1);
    }

    /**
     * The race the grace window exists for: two tabs whose access tokens expired
     * together, both presenting the same refresh token within moments.
     */
    @Test
    void twoTabsRefreshingTogetherBothKeepWorking() {
        AuthTokens shared = userService.login(email, PASSWORD);

        AuthTokens tabA = userService.refresh(shared.refreshToken());
        AuthTokens tabB = userService.refresh(shared.refreshToken());

        assertThat(tabA.refreshToken()).isNotEqualTo(tabB.refreshToken());
        assertThatNoException().isThrownBy(() -> userService.refresh(tabA.refreshToken()));
        assertThatNoException().isThrownBy(() -> userService.refresh(tabB.refreshToken()));
    }

    @Test
    void aTokenPresentedLongAfterRotationEndsEverySessionFromThatSignIn() {
        AuthTokens first = userService.login(email, PASSWORD);
        AuthTokens second = userService.refresh(first.refreshToken());

        pretendTheRotationWasLongAgo();

        assertThatThrownBy(() -> userService.refresh(first.refreshToken()))
                .isInstanceOf(UnauthenticatedException.class);

        assertThatThrownBy(() -> userService.refresh(second.refreshToken()))
                .as("the honest holder is signed out too - the server cannot tell them apart")
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void signingOutEndsTheSession() {
        AuthTokens signedIn = userService.login(email, PASSWORD);

        userService.logout(signedIn.refreshToken());

        assertThatThrownBy(() -> userService.refresh(signedIn.refreshToken()))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void signingOutWithATokenWeNeverIssuedIsSilent() {
        assertThatNoException().isThrownBy(() -> userService.logout("never-issued"));
    }
}