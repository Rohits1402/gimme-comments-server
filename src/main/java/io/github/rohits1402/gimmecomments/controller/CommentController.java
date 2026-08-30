package io.github.rohits1402.gimmecomments.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.rohits1402.gimmecomments.dto.CommentResponse;
import io.github.rohits1402.gimmecomments.dto.CreateCommentRequest;
import io.github.rohits1402.gimmecomments.dto.LikeResponse;
import io.github.rohits1402.gimmecomments.dto.UpdateCommentRequest;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.CommentLike;
import io.github.rohits1402.gimmecomments.service.CommentService;
import io.github.rohits1402.gimmecomments.service.LikeService;
import io.github.rohits1402.gimmecomments.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final UserService userService;
    private final CommentService commentService;
    private final LikeService likeService;

    record CommentListEnvelope(List<CommentResponse> comments,
                               @JsonProperty("next_cursor")
                               @JsonInclude(JsonInclude.Include.NON_NULL) String nextCursor,
                               @JsonProperty("total_comments") long totalComments) {
    }

    record CommentMsgEnvelope(String msg, CommentResponse comment) {
    }

    record LikeEnvelope(LikeResponse like) {

    }

    record MsgEnvelope(String msg) {
    }

    public CommentController(UserService userService, CommentService commentService, LikeService likeService) {
        this.userService = userService;
        this.commentService = commentService;
        this.likeService = likeService;
    }

    @DeleteMapping("/comment/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal String userId, @PathVariable String commentId) {
        commentService.deleteComment(userId, commentId);
    }


    @GetMapping("/comment/{websiteId}")
    public CommentListEnvelope getAll(@PathVariable String websiteId,
                                      @AuthenticationPrincipal String callerUserId,
                                      @RequestParam(required = false) String cursor,
                                      @RequestParam(required = false) Integer size) {
        CommentService.CommentPage page =
                commentService.getPageForWebsite(websiteId, callerUserId, cursor, size);

        List<CommentResponse> list = page.comments().stream()
                .map(c -> CommentResponse.from(c.comment(), c.comment().getAuthor(), c.likedBy(), c.iLiked()))
                .toList();

        return new CommentListEnvelope(list, page.nextCursor(), page.total());
    }

    @PostMapping("/comment/{websiteId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentMsgEnvelope create(@AuthenticationPrincipal String userId, @PathVariable String websiteId,
                                     @Valid @RequestBody CreateCommentRequest request) {
        Comment created = commentService.create(userId, websiteId,
                request.commentDescription(), request.commentParent());
        return new CommentMsgEnvelope("Comment created successfully", CommentResponse.from(created, userService.getById(userId)));
    }

    @PatchMapping("/comment/{commentId}")
    public CommentMsgEnvelope update(@AuthenticationPrincipal String userId, @PathVariable String commentId,
                                     @Valid @RequestBody UpdateCommentRequest request) {
        Comment updated = commentService.update(userId, commentId, request.commentDescription());
        return new CommentMsgEnvelope("Comment updated successfully", CommentResponse.from(updated, userService.getById(userId)));
    }

    @PostMapping("/like/{commentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public LikeEnvelope createLike(@AuthenticationPrincipal String userId, @PathVariable String commentId) {
        CommentLike created = likeService.create(userId, commentId);
        return new LikeEnvelope(LikeResponse.from(created));
    }

    @DeleteMapping("/like/{commentId}")
    public MsgEnvelope deleteLike(@AuthenticationPrincipal String userId, @PathVariable String commentId) {
        likeService.delete(userId, commentId);
        return new MsgEnvelope("Like removed successfully");
    }
}
