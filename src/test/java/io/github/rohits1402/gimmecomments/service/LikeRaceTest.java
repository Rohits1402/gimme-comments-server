package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.TestDatabase;
import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.CommentLike;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.repository.CommentLikeRepository;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.LikeCount;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Liking a comment checks whether a like already exists and then inserts one. Between
 * those two steps another request can insert the same like, so the database's unique
 * index — not the check — is what actually enforces the rule.
 * <p>
 * This test is deliberately NOT @Transactional. The threads need to see each other's
 * committed work, and a test-held transaction would hide the setup rows from them.
 * Everything it creates is removed afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
class LikeRaceTest {

    @Autowired
    private LikeService likeService;
    @Autowired
    private CommentLikeRepository likes;
    @Autowired
    private CommentRepository comments;
    @Autowired
    private WebsiteRepository websites;
    @Autowired
    private UserRepository users;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private User user;
    private Website site;
    private Comment comment;

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setName("Racer");
        u.setEmail("racer-" + UUID.randomUUID() + "@example.test");
        u.setPassword("irrelevant");
        user = users.save(u);

        Website w = new Website();
        w.setOwner(user);
        w.setName("Race site");
        w.setDescription("");
        w.setUrl("https://" + UUID.randomUUID() + ".example.test");
        w.setWebsiteConfiguration(Map.of());
        site = websites.save(w);

        Comment c = new Comment();
        c.setAuthor(user);
        c.setWebsite(site);
        c.setCommentDescription("Like me twice");
        comment = comments.save(c);
    }

    @AfterEach
    void tearDown() {
        // Likes and replies go with the comment, by cascade in the database.
        comments.deleteById(comment.getId());
        websites.deleteById(site.getId());
        users.deleteById(user.getId());
    }

    @Test
    void aLikeThatLosesTheRaceGetsTheSameAnswerAsAnObviousDuplicate() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch rowIsInPlace = new CountDownLatch(1);
        CountDownLatch holderMayCommit = new CountDownLatch(1);

        // A like that exists in the index but is committed by nobody, so no other
        // transaction can see it — which is precisely the state the real race creates.
        Future<?> holder = pool.submit(() ->
                new TransactionTemplate(transactionManager).execute(status -> {
                    CommentLike like = new CommentLike();
                    like.setComment(comments.getReferenceById(comment.getId()));
                    like.setUser(users.getReferenceById(user.getId()));
                    likes.saveAndFlush(like);
                    rowIsInPlace.countDown();
                    awaitQuietly(holderMayCommit);
                    return null;
                }));

        assertThat(rowIsInPlace.await(10, TimeUnit.SECONDS)).isTrue();

        // The real service. Its check sees nothing, so it inserts, and its insert
        // then waits on the unique index for the holder to finish.
        Future<Throwable> loser = pool.submit(() -> catchThrowable(
                () -> likeService.create(user.getId().toString(), comment.getId().toString())));

        // Long enough for that thread to be past the check and waiting on the index.
        // If it were too short the check would win instead, and the assertions below
        // would still hold — the wait decides which path runs, not whether it passes.
        Thread.sleep(500);
        holderMayCommit.countDown();
        holder.get(10, TimeUnit.SECONDS);

        Throwable thrown = loser.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(likes.countByCommentIds(List.of(comment.getId())))
                .as("the database must hold the line however the application behaves")
                .singleElement()
                .extracting(LikeCount::total)
                .isEqualTo(1L);

        assertThat(thrown)
                .as("losing a race is still just a duplicate, and must read like one")
                .isInstanceOf(BadRequestException.class);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}