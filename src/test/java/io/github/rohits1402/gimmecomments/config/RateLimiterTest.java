package io.github.rohits1402.gimmecomments.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bucket itself, with no Spring around it. This is where the counting rules are,
 * so this is where they are checked.
 */
class RateLimiterTest {

    private final RateLimiter limiter = new RateLimiter();

    @Test
    void allowsUpToCapacityThenRefuses() {
        for (int i = 1; i <= 3; i++) {
            assertThat(limiter.allow("someone", 3, Duration.ofMinutes(1)))
                    .as("request %d of 3", i)
                    .isTrue();
        }
        assertThat(limiter.allow("someone", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void oneCallerRunningOutDoesNotAffectAnother() {
        limiter.allow("noisy", 1, Duration.ofMinutes(1));
        assertThat(limiter.allow("noisy", 1, Duration.ofMinutes(1))).isFalse();

        assertThat(limiter.allow("quiet", 1, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void tokensComeBackOverTime() throws InterruptedException {
        // Two per second, so a token is due back about half a second after it is spent.
        assertThat(limiter.allow("waiter", 2, Duration.ofSeconds(1))).isTrue();
        assertThat(limiter.allow("waiter", 2, Duration.ofSeconds(1))).isTrue();
        assertThat(limiter.allow("waiter", 2, Duration.ofSeconds(1))).isFalse();

        Thread.sleep(700);

        assertThat(limiter.allow("waiter", 2, Duration.ofSeconds(1)))
                .as("a token should have refilled by now")
                .isTrue();
    }

    @Test
    void idleCallersAreForgottenButBusyOnesAreKept() {
        limiter.allow("spent", 1, Duration.ofHours(1));   // empty, and slow to refill
        limiter.allow("untouched", 5, Duration.ofHours(1));
        // "untouched" spent one of five, so it is not full either — take the rest.
        for (int i = 0; i < 4; i++) limiter.allow("untouched", 5, Duration.ofHours(1));

        limiter.forgetIdleCallers();

        // Both are empty, so both must survive: forgetting them would hand back a
        // fresh allowance to whoever just used theirs up.
        assertThat(limiter.allow("spent", 1, Duration.ofHours(1))).isFalse();
        assertThat(limiter.allow("untouched", 5, Duration.ofHours(1))).isFalse();
    }
}