package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.CommentResponse;
import io.github.rohits1402.gimmecomments.dto.CreateCommentRequest;
import io.github.rohits1402.gimmecomments.dto.LikeResponse;
import io.github.rohits1402.gimmecomments.dto.UpdateCommentRequest;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.Like;
import io.github.rohits1402.gimmecomments.model.jpa.User;
import io.github.rohits1402.gimmecomments.service.CommentService;
import io.github.rohits1402.gimmecomments.service.LikeService;
import io.github.rohits1402.gimmecomments.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final UserService userService;
    private final CommentService commentService;
    private final LikeService likeService;

    record CommentListEnvelope(List<CommentResponse> comments) {
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
    public CommentListEnvelope getAll(@PathVariable String websiteId) {
        List<Comment> comments = commentService.getAllForWebsite(websiteId);

        List<String> authorIds = comments.stream().map(Comment::getUserId).distinct().toList();
        Map<String, User> authors = userService.getAllByIds(authorIds);

        List<CommentResponse> list = comments.stream()
                .map(comment -> CommentResponse.from(comment, authors.get(comment.getUserId())))
                .toList();
        return new CommentListEnvelope(list);
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
        return new CommentMsgEnvelope("Comment updated successfully",CommentResponse.from(updated, userService.getById(userId)));
    }

    @PostMapping("/like/{commentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public LikeEnvelope createLike(@AuthenticationPrincipal String userId, @PathVariable String commentId) {
        Like created = likeService.create(userId, commentId);
        return new LikeEnvelope(LikeResponse.from(created));
    }

    @DeleteMapping("/like/{commentId}")
    public MsgEnvelope deleteLike(@AuthenticationPrincipal String userId, @PathVariable String commentId) {
        likeService.delete(userId, commentId);
        return new MsgEnvelope("Like removed successfully");
    }
}
