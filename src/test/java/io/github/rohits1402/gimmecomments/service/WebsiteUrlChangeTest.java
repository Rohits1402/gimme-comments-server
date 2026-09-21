package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.TestDatabase;
import io.github.rohits1402.gimmecomments.exception.ConflictException;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A website's address could not be changed, so moving a domain meant deleting the site
 * and adding it again - which threw away every comment on it. Comments belong to the
 * website's id, never its address, so there was never a reason for that.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
@Transactional
class WebsiteUrlChangeTest {

    @Autowired
    private WebsiteService websites;
    @Autowired
    private UserRepository users;
    @Autowired
    private WebsiteRepository websiteRepository;
    @Autowired
    private CommentRepository comments;

    private User owner;

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setName("Mover");
        u.setEmail("mover-" + UUID.randomUUID() + "@example.test");
        u.setPassword("irrelevant");
        owner = users.save(u);
    }

    private Website site(String url) {
        Website w = new Website();
        w.setOwner(owner);
        w.setName("Site");
        w.setDescription("");
        w.setUrl(url);
        w.setWebsiteConfiguration(Map.of());
        return websiteRepository.save(w);
    }

    @Test
    void movingToANewAddressKeepsEveryComment() {
        Website w = site("https://old-" + UUID.randomUUID() + ".example.test");

        Comment c = new Comment();
        c.setAuthor(owner);
        c.setWebsite(w);
        c.setCommentDescription("still here afterwards");
        comments.save(c);

        String newAddress = "https://new-" + UUID.randomUUID() + ".example.test";
        websites.update(w.getId().toString(), owner.getId().toString(),
                null, null, newAddress, null);

        assertThat(websiteRepository.findById(w.getId()).orElseThrow().getUrl())
                .isEqualTo(newAddress);
        assertThat(comments.countByWebsiteId(w.getId()))
                .as("comments belong to the site, not to its address")
                .isEqualTo(1);
    }

    /**
     * The unique constraint on the URL is what actually stops two sites sharing an
     * address. Left to surface at commit time it would arrive as the generic
     * "Resource already exists"; caught here it reads the same as creating a duplicate.
     */
    @Test
    void movingOntoAnAddressSomebodyElseHasReadsLikeAnyOtherDuplicate() {
        Website taken = site("https://taken-" + UUID.randomUUID() + ".example.test");
        Website mine = site("https://mine-" + UUID.randomUUID() + ".example.test");

        assertThatThrownBy(() -> websites.update(mine.getId().toString(), owner.getId().toString(),
                null, null, taken.getUrl(), null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Website already exist!");
    }
}
