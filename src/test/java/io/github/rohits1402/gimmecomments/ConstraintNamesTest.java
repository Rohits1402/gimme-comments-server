package io.github.rohits1402.gimmecomments;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static io.github.rohits1402.gimmecomments.exception.ConstraintViolations.ONE_LIKE_PER_USER;
import static io.github.rohits1402.gimmecomments.exception.ConstraintViolations.USER_EMAIL;
import static io.github.rohits1402.gimmecomments.exception.ConstraintViolations.WEBSITE_URL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The services match these names at runtime to turn a lost race into the same answer
 * the up-front check gives. Rename one in a migration and nothing fails - the match
 * just stops happening and the API silently goes back to "Resource already exists".
 * There is no way to notice that except to assert the names.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
class ConstraintNamesTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theConstraintNamesTheCodeReliesOnAreTheOnesInTheSchema() {
        List<String> unique = jdbc.queryForList(
                "SELECT conname FROM pg_constraint WHERE contype = 'u' "
                        + "AND connamespace = 'public'::regnamespace", String.class);

        assertThat(unique).contains(USER_EMAIL, WEBSITE_URL, ONE_LIKE_PER_USER);
    }
}