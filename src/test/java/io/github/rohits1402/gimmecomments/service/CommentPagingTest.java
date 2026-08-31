package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.TestDatabase;
import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Paging is tested against a real database because every guarantee it makes lives in
 * SQL: the limit, the cursor comparison and the thread lookup. A mocked repository
 * returns whatever the test told it to and would prove none of them.
 * <p>
 * Runs inside a transaction that is rolled back, so the development database is left
 * exactly as it was found.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
@Transactional
class CommentPagingTest {

    @Autowired
    private CommentService commentService;
    @Autowired
    private UserRepository users;
    @Autowired
    private WebsiteRepository websites;
    @Autowired
    private CommentRepository comments;

    private User author;
    private Website site;

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setName("Paging");
        u.setEmail("paging-" + UUID.randomUUID() + "@example.test");
        u.setPassword("irrelevant");
        author = users.save(u);

        Website w = new Website();
        w.setOwner(author);
        w.setName("Paging site");
        w.setDescription("");
        w.setUrl("https://" + UUID.randomUUID() + ".example.test");
        w.setWebsiteConfiguration(Map.of());
        site = websites.save(w);
    }

    private Comment write(Comment parent, String body) {
        Comment c = new Comment();
        c.setAuthor(author);
        c.setWebsite(site);
        c.setParent(parent);
        c.setCommentDescription(body);
        return comments.save(c);
    }

    private String siteId() {
        return site.getId().toString();
    }

    private static List<Comment> commentsIn(CommentService.CommentPage page) {
        return page.comments().stream().map(CommentWithLikes::comment).toList();
    }

    private static List<Comment> rootsIn(CommentService.CommentPage page) {
        return commentsIn(page).stream().filter(c -> c.getParent() == null).toList();
    }

    /**
     * The invariant the whole design rests on. It is enforced by a @PrePersist callback
     * on the entity, so saving straight through the repository - as this test does, and
     * as OverviewOwnerIsolationTest does - still has to produce it.
     */
    @Test
    void everyCommentJoinsAThread_evenSavedStraightThroughTheRepository() {
        Comment root = write(null, "opener");
        Comment reply = write(root, "reply");
        Comment deeper = write(reply, "reply to the reply");

        assertThat(root.getRootComment().getId()).isEqualTo(root.getId());
        assertThat(reply.getRootComment().getId()).isEqualTo(root.getId());
        assertThat(deeper.getRootComment().getId()).isEqualTo(root.getId());
    }

    /**
     * The failure this prevents is silent: a reply sent without its parent is dropped by
     * the widget while the response is still 200, so the comment is simply not on the
     * page and nothing reports an error.
     */
    @Test
    void aPageCarriesWholeThreads_neverAReplyWithoutItsParent() {
        for (int i = 0; i < 3; i++) {
            Comment root = write(null, "thread " + i);
            Comment reply = write(root, "reply on " + i);
            write(reply, "deeper on " + i);
        }

        CommentService.CommentPage page = commentService.getPageForWebsite(siteId(), null, null, 1);

        assertThat(rootsIn(page)).hasSize(1);
        UUID thread = rootsIn(page).getFirst().getId();

        // Three comments, and all of them belong to the one thread the page opened.
        assertThat(commentsIn(page)).hasSize(3);
        assertThat(commentsIn(page))
                .allSatisfy(c -> assertThat(c.getRootComment().getId()).isEqualTo(thread));
    }

    /**
     * Following the cursor to the end must visit every thread exactly once. Skipping and
     * repeating are the two ways paging goes wrong, and this catches both at once. The
     * page counter makes it fail rather than hang if the cursor ever stops advancing.
     */
    @Test
    void followingTheCursorVisitsEveryThreadExactlyOnce() {
        for (int i = 0; i < 7; i++) {
            write(null, "thread " + i);
        }

        List<UUID> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;

        do {
            CommentService.CommentPage page =
                    commentService.getPageForWebsite(siteId(), null, cursor, 2);
            rootsIn(page).forEach(c -> seen.add(c.getId()));
            cursor = page.nextCursor();
            pages++;
            assertThat(pages).as("the cursor stopped advancing").isLessThan(10);
        } while (cursor != null);

        assertThat(seen).hasSize(7);
        assertThat(Set.copyOf(seen)).as("a thread was sent twice").hasSize(7);
        assertThat(pages).isEqualTo(4);   // 2 + 2 + 2 + 1
    }

    /** total_comments counts everything on the site, not what fitted on the page. */
    @Test
    void theTotalIgnoresThePage() {
        for (int i = 0; i < 4; i++) {
            write(write(null, "thread " + i), "reply " + i);
        }

        CommentService.CommentPage page = commentService.getPageForWebsite(siteId(), null, null, 1);

        assertThat(page.total()).isEqualTo(8);
        assertThat(commentsIn(page)).hasSize(2);
        assertThat(page.nextCursor()).isNotNull();
    }

    /** The last page says so by carrying no cursor at all. */
    @Test
    void theLastPageHasNoCursor() {
        write(null, "the only thread");

        CommentService.CommentPage page = commentService.getPageForWebsite(siteId(), null, null, 20);

        assertThat(page.nextCursor()).isNull();
    }

    /**
     * How much work one request may cost is our decision, not the caller's. Without the
     * upper bound, size=1000000 is a free way to undo everything paging bought.
     */
    @Test
    void theCallerCannotChooseThePageSize() {
        for (int i = 0; i < 101; i++) {
            write(null, "thread " + i);
        }

        assertThat(rootsIn(commentService.getPageForWebsite(siteId(), null, null, 1_000_000)))
                .hasSize(100);
        assertThat(rootsIn(commentService.getPageForWebsite(siteId(), null, null, 0)))
                .hasSize(1);
        assertThat(rootsIn(commentService.getPageForWebsite(siteId(), null, null, -5)))
                .hasSize(1);
    }

    /** A cursor we did not write is the caller's mistake, and must not become a 500. */
    @Test
    void aCursorWeDidNotWriteIsRejected() {
        write(null, "something to page over");

        assertThatThrownBy(() -> commentService.getPageForWebsite(siteId(), null, "not-a-cursor", 20))
                .isInstanceOf(BadRequestException.class);

        // Valid Base64, but not our contents.
        assertThatThrownBy(() -> commentService.getPageForWebsite(siteId(), null, "aGVsbG8", 20))
                .isInstanceOf(BadRequestException.class);
    }

    /** An id that is not a UUID is an empty page, which is what it was before paging. */
    @Test
    void anIdThatIsNotAUuidIsAnEmptyPage() {
        CommentService.CommentPage page =
                commentService.getPageForWebsite("not-a-uuid", null, null, 20);

        assertThat(page.comments()).isEmpty();
        assertThat(page.total()).isZero();
        assertThat(page.nextCursor()).isNull();
    }
}
