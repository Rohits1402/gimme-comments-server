package io.github.rohits1402.gimmecomments.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * V6 adds root_comment_id and fills it for rows that already existed, by walking down
 * from every thread opener. Every other test starts from an empty database, where that
 * walk updates nothing - so this is the only place it runs at all.
 * <p>
 * A wrong backfill is not necessarily a loud one. Giving every comment itself as its
 * root leaves no NULLs, so the migration's final NOT NULL check still passes, and the
 * damage only shows up later as replies quietly missing from every page.
 * <p>
 * Deliberately built from raw SQL and its own container, with no Spring context: the
 * entity classes describe the newest schema and cannot write to the one that existed at
 * version 5, and a test that pins V6 should still pass in a year after the entities
 * have moved on.
 */
class CommentRootBackfillTest {

    @Test
    void everyExistingCommentIsGivenTheTopOfItsOwnThread() throws SQLException {
        try (PostgreSQLContainer db = new PostgreSQLContainer("postgres:17")) {
            db.start();

            migrateTo(db, "5");

            try (Connection c = connect(db)) {
                UUID author = insertUser(c);
                UUID site = insertWebsite(c, author);

                // One deep chain: the recursion has to carry the opener all the way
                // down, not just to the first level.
                UUID a0 = insertComment(c, site, author, null, "A root");
                UUID a1 = insertComment(c, site, author, a0, "A reply");
                UUID a2 = insertComment(c, site, author, a1, "A reply to the reply");
                insertComment(c, site, author, a2, "A reply three deep");

                // A second thread, so a wrong join would mix them together.
                UUID b0 = insertComment(c, site, author, null, "B root");
                insertComment(c, site, author, b0, "B reply");

                // And one with nothing under it, which must end up pointing at itself.
                insertComment(c, site, author, null, "C root, no replies");
            }

            migrateTo(db, "6");

            try (Connection c = connect(db)) {
                assertThat(rootOfEachComment(c)).containsOnly(
                        entry("A root", "A root"),
                        entry("A reply", "A root"),
                        entry("A reply to the reply", "A root"),
                        entry("A reply three deep", "A root"),
                        entry("B root", "B root"),
                        entry("B reply", "B root"),
                        entry("C root, no replies", "C root, no replies"));
            }
        }
    }

    private static void migrateTo(PostgreSQLContainer db, String version) {
        Flyway.configure()
                .dataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword())
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private static Connection connect(PostgreSQLContainer db) throws SQLException {
        return DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
    }

    private static UUID insertUser(Connection c) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (id, name, email, password) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setString(2, "Backfill");
            ps.setString(3, "backfill-" + id + "@example.test");
            ps.setString(4, "irrelevant");
            ps.executeUpdate();
        }
        return id;
    }

    private static UUID insertWebsite(Connection c, UUID owner) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO websites (id, name, url, user_id) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setString(2, "Backfill site");
            ps.setString(3, "https://" + id + ".example.test");
            ps.setObject(4, owner);
            ps.executeUpdate();
        }
        return id;
    }

    private static UUID insertComment(Connection c, UUID site, UUID author, UUID parent, String body)
            throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO comments (id, website_id, user_id, parent_comment_id, comment_description) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, site);
            ps.setObject(3, author);
            if (parent == null) {
                ps.setNull(4, Types.OTHER);
            } else {
                ps.setObject(4, parent);
            }
            ps.setString(5, body);
            ps.executeUpdate();
        }
        return id;
    }

    /**
     * Each comment paired with the text of whatever V6 decided its thread opener is.
     */
    private static Map<String, String> rootOfEachComment(Connection c) throws SQLException {
        Map<String, String> rootOf = new LinkedHashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT c.comment_description, r.comment_description "
                             + "FROM comments c JOIN comments r ON r.id = c.root_comment_id")) {
            while (rs.next()) {
                rootOf.put(rs.getString(1), rs.getString(2));
            }
        }
        return rootOf;
    }
}