package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.CommentResponse;
import io.github.rohits1402.gimmecomments.dto.CreateCommentRequest;
import io.github.rohits1402.gimmecomments.dto.UpdateCommentRequest;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;

    record CommentListEnvelope(List<CommentResponse> comments) {
    }

    record CommentMsgEnvelope(String msg, CommentResponse comment) {
    }

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @DeleteMapping("/comment/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable String commentId) {
        commentService.deleteComment(commentId);
    }


    @GetMapping("/comment/{websiteId}")
    public CommentListEnvelope getAll(@PathVariable String websiteId) {
        List<CommentResponse> list = commentService.getAllForWebsite(websiteId).stream()
                .map(CommentResponse::from)
                .toList();
        return new CommentListEnvelope(list);
    }

    @PostMapping("/comment/{websiteId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentMsgEnvelope create(@PathVariable String websiteId,
                                     @Valid @RequestBody CreateCommentRequest request) {
        Comment created = commentService.create(request.userId(), websiteId,
                request.commentDescription(), request.commentParent());
        return new CommentMsgEnvelope("Comment created successfully", CommentResponse.from(created));
    }

    @PatchMapping("/comment/{commentId}")
    public CommentMsgEnvelope update(@PathVariable String commentId,
                                     @Valid @RequestBody UpdateCommentRequest request) {
        Comment updated = commentService.update(commentId, request.commentDescription());
        return new CommentMsgEnvelope("Comment updated successfully", CommentResponse.from(updated));
    }


}
