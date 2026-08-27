package io.github.rohits1402.gimmecomments.config;

import io.github.bucket4j.Bucket;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * One token bucket per caller, held in memory.
 * <p>
 * In memory means each running copy of the application counts separately: with three
 * instances behind a load balancer, a limit of three per minute is really nine. That
 * is honest here because Render runs one instance, and the thing to change if that
 * ever stops being true is this class, not its callers.
 */
@Component
public class RateLimiter {

    /** The bucket, plus the capacity it was built with — see {@link #forgetIdleCallers}. */
    private record Tracked(Bucket bucket, long capacity) {
    }

    private final Map<String, Tracked> buckets = new ConcurrentHashMap<>();

    /**
     * Takes one token for {@code key}, creating its bucket on first sight.
     *
     * @return true when the caller may proceed, false when they have run out
     */
    public boolean allow(String key, long capacity, java.time.Duration per) {
        Tracked tracked = buckets.computeIfAbsent(key, k -> new Tracked(
                Bucket.builder()
                        .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, per))
                        .build(),
                capacity));

        return tracked.bucket().tryConsume(1);
    }

    /**
     * Without this the map gains an entry for every address ever seen and never loses
     * one — a slow leak that only appears under the traffic you were hoping for.
     * <p>
     * A bucket sitting at full capacity has had nothing taken from it recently enough
     * to matter, so it holds no state worth keeping. Dropping it is the same as never
     * having seen that caller.
     */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void forgetIdleCallers() {
        buckets.entrySet().removeIf(e -> e.getValue().bucket().getAvailableTokens() >= e.getValue().capacity());
    }
}