package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.dto.OverviewResponse;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The overview is the one endpoint that deliberately reaches across every website a
 * person owns. That makes it the one place where a missing filter would leak another
 * customer's comments, so the boundary is tested against a real database rather than
 * trusted to a mocked repository — the filter lives in JPQL, and a mock cannot check
 * JPQL.
 * <p>
 * Runs inside a transaction that is rolled back, so it leaves the development database
 * exactly as it found it.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class OverviewOwnerIsolationTest {

    @Autowired
    private OverviewService overview;
    @Autowired
    private UserRepository users;
    @Autowired
    private WebsiteRepository websites;
    @Autowired
    private CommentRepository comments;

    private User newUser(String name) {
        User u = new User();
        u.setName(name);
        u.setEmail(name + "-" + UUID.randomUUID() + "@example.test");
        u.setPassword("irrelevant");
        return users.save(u);
    }

    private Website newSite(User owner, String name) {
        Website w = new Website();
        w.setOwner(owner);
        w.setName(name);
        w.setDescription("");
        w.setUrl("https://" + UUID.randomUUID() + ".example.test");
        w.setWebsiteConfiguration(Map.of());
        return websites.save(w);
    }

    private void newComment(User author, Website site, String body) {
        Comment c = new Comment();
        c.setAuthor(author);
        c.setWebsite(site);
        c.setCommentDescription(body);
        comments.save(c);
    }

    @Test
    void anOwnerSeesOnlyTheirOwnWebsitesActivity() {
        User alice = newUser("alice");
        User bob = newUser("bob");
        User reader = newUser("reader");

        Website aliceSite = newSite(alice, "Alice site");
        Website bobSite = newSite(bob, "Bob site");

        newComment(reader, aliceSite, "on alice");
        newComment(reader, bobSite, "on bob one");
        newComment(reader, bobSite, "on bob two");

        OverviewResponse forAlice = overview.load(alice.getId().toString());
        OverviewResponse forBob = overview.load(bob.getId().toString());

        assertThat(forAlice.totals().comments()).isEqualTo(1);
        assertThat(forAlice.totals().websites()).isEqualTo(1);
        assertThat(forAlice.recent())
                .extracting(r -> r.website().websiteName())
                .containsOnly("Alice site");

        assertThat(forBob.totals().comments()).isEqualTo(2);
        assertThat(forBob.recent())
                .extracting(r -> r.website().websiteName())
                .containsOnly("Bob site");
    }

    @Test
    void writingCommentsOnOtherPeoplesSitesGivesYouNoOverviewOfYourOwn() {
        User owner = newUser("owner");
        User reader = newUser("prolific-reader");

        Website site = newSite(owner, "Owner site");
        newComment(reader, site, "one");
        newComment(reader, site, "two");

        // The reader wrote every comment in the system, and owns nothing.
        OverviewResponse forReader = overview.load(reader.getId().toString());

        assertThat(forReader.totals().comments()).isZero();
        assertThat(forReader.totals().websites()).isZero();
        assertThat(forReader.recent()).isEmpty();
    }

    @Test
    void aCallerWhoIsNotAUuidGetsAnEmptyOverviewRatherThanAnError() {
        // Anonymous requests arrive as the string "anonymousUser".
        OverviewResponse none = overview.load("anonymousUser");

        assertThat(none.totals().comments()).isZero();
        assertThat(none.daily()).isEmpty();
        assertThat(none.recent()).isEmpty();
    }

    @Test
    void theActivityStripAlwaysCoversFourteenDaysIncludingQuietOnes() {
        User owner = newUser("chart-owner");
        newSite(owner, "Chart site");

        OverviewResponse response = overview.load(owner.getId().toString());

        assertThat(response.daily()).hasSize(14);
        assertThat(response.daily()).allSatisfy(d -> assertThat(d.count()).isZero());
        assertThat(response.daily().get(13).day())
                .isAfter(response.daily().get(0).day());
    }
}
