package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.repository.CommentLikeRepository;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who is allowed to delete a comment. The rule involves two different owners — the
 * author of the comment and the owner of the website it sits on — which is more than
 * a controller slice with a mocked service can check.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final UUID COMMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID AUTHOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SITE_OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID STRANGER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private CommentRepository comments;
    @Mock
    private UserRepository users;
    @Mock
    private WebsiteRepository websites;
    @Mock
    private WebsiteService websiteService;
    @Mock
    private CommentLikeRepository likes;

    @InjectMocks
    private CommentService service;

    private Comment comment;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setId(AUTHOR_ID);

        User siteOwner = new User();
        siteOwner.setId(SITE_OWNER_ID);

        Website website = new Website();
        website.setId(UUID.randomUUID());
        website.setOwner(siteOwner);

        comment = new Comment();
        comment.setId(COMMENT_ID);
        comment.setAuthor(author);
        comment.setWebsite(website);
        comment.setCommentDescription("Hello");
    }

    @Test
    void theAuthorCanDeleteTheirOwnComment() {
        when(comments.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        service.deleteComment(AUTHOR_ID.toString(), COMMENT_ID.toString());

        verify(comments).deleteById(COMMENT_ID);
    }

    @Test
    void theWebsiteOwnerCanDeleteSomeoneElsesComment() {
        when(comments.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        service.deleteComment(SITE_OWNER_ID.toString(), COMMENT_ID.toString());

        verify(comments).deleteById(COMMENT_ID);
    }

    @Test
    void aStrangerCannotDeleteAndIsNotToldTheCommentExists() {
        when(comments.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.deleteComment(STRANGER_ID.toString(), COMMENT_ID.toString()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Comment not found");

        verify(comments, never()).deleteById(COMMENT_ID);
    }

    @Test
    void anOrphanedCommentCanStillBeRemovedByTheWebsiteOwner() {
        // The author deleted their account, so comments.user_id went to NULL.
        comment.setAuthor(null);
        when(comments.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        service.deleteComment(SITE_OWNER_ID.toString(), COMMENT_ID.toString());

        verify(comments).deleteById(COMMENT_ID);
    }

    @Test
    void editingStaysAuthorOnly_theWebsiteOwnerMayRemoveWordsButNotRewriteThem() {
        when(comments.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() ->
                service.update(SITE_OWNER_ID.toString(), COMMENT_ID.toString(), "edited"))
                .isInstanceOf(NotFoundException.class);

        assertThat(comment.getCommentDescription()).isEqualTo("Hello");
    }

    @Test
    void aMalformedCommentIdIsANotFound_notAnIllegalArgument() {
        assertThatThrownBy(() -> service.deleteComment(AUTHOR_ID.toString(), "not-a-uuid"))
                .isInstanceOf(NotFoundException.class);
    }
}
