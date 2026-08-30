package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.config.RateLimiter;
import io.github.rohits1402.gimmecomments.config.SecurityConfig;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.service.CommentService;
import io.github.rohits1402.gimmecomments.service.CommentWithLikes;
import io.github.rohits1402.gimmecomments.service.JwtService;
import io.github.rohits1402.gimmecomments.service.LikeService;
import io.github.rohits1402.gimmecomments.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, RateLimiter.class})
class CommentControllerTest {

    private static final String WEBSITE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String USER_ID = "22222222-2222-2222-2222-222222222222";
    private static final String COMMENT_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private LikeService likeService;
    @MockitoBean
    private JwtService jwtService;

    private static UsernamePasswordAuthenticationToken callerIs(String userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    private static User sampleAuthor() {
        User author = new User();
        author.setId(UUID.fromString(USER_ID));
        author.setName("Rohit");
        return author;
    }

    private static Comment sampleComment() {
        Website site = new Website();
        site.setId(UUID.fromString(WEBSITE_ID));

        Comment c = new Comment();
        c.setId(UUID.fromString(COMMENT_ID));
        c.setAuthor(sampleAuthor());
        c.setWebsite(site);
        c.setCommentDescription("Hi");
        c.setCreatedAt(Instant.parse("2026-08-24T12:00:00Z"));
        return c;
    }

    @Test
    void listComments_carriesTheLikeCountAndWhetherTheCallerLiked() throws Exception {
        when(commentService.getPageForWebsite(WEBSITE_ID, USER_ID, null, null))
                .thenReturn(new CommentService.CommentPage(
                        List.of(new CommentWithLikes(sampleComment(), 3L, true)), "next-page", 41));

        mockMvc.perform(get("/api/v1/comments/comment/" + WEBSITE_ID)
                        .with(authentication(callerIs(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].liked_by").value(3))
                .andExpect(jsonPath("$.comments[0].i_liked").value(true))
                .andExpect(jsonPath("$.next_cursor").value("next-page"))
                .andExpect(jsonPath("$.total_comments").value(41));
    }

    @Test
    void listComments_areReadableWithoutAToken_andTheCallerIsAnonymous() throws Exception {
        when(commentService.getPageForWebsite(WEBSITE_ID, "anonymousUser", null, null))
                .thenReturn(new CommentService.CommentPage(
                        List.of(new CommentWithLikes(sampleComment(), 5L, false)), null, 1));

        mockMvc.perform(get("/api/v1/comments/comment/" + WEBSITE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].liked_by").value(5))
                .andExpect(jsonPath("$.comments[0].i_liked").value(false))
                // Absent, not null: the last page carries no cursor at all.
                .andExpect(jsonPath("$.next_cursor").doesNotExist());
    }

    @Test
    void createComment_sendsNoLikeFields_butKeepsCommentParent() throws Exception {
        when(commentService.create(eq(USER_ID), eq(WEBSITE_ID), eq("Hi"), isNull()))
                .thenReturn(sampleComment());
        when(userService.getById(USER_ID)).thenReturn(sampleAuthor());

        mockMvc.perform(post("/api/v1/comments/comment/" + WEBSITE_ID)
                        .with(authentication(callerIs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment_description\": \"Hi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment.liked_by").doesNotExist())
                .andExpect(jsonPath("$.comment.i_liked").doesNotExist())
                .andExpect(content().string(containsString("\"comment_parent\"")));
    }
}