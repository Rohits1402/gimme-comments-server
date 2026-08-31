package io.github.rohits1402.gimmecomments;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A throwaway PostgreSQL for the tests that need a real one.
 * <p>
 * The container is a Spring bean rather than a JUnit @Container, so Spring owns
 * starting and stopping it — and because Spring caches the application context
 * between test classes, one container serves the whole run instead of one per class.
 * <p>
 * The image tag is pinned to the version production runs. A test database that is a
 * different version from the real one can pass while the migration it is proving
 * would fail on deploy.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestDatabase {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17");
    }
}