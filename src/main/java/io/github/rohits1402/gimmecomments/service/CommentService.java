package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.LikeRepository;
import org.springframework.stereotype.Service;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final LikeRepository likes;

    public CommentService(CommentRepository comments, LikeRepository likes) {
        this.comments = comments;
        this.likes = likes;
    }


    public void deleteComment(String commentId) {
        if (!comments.existsById(commentId))
            throw new NotFoundException("Comment not found");

        deleteTree(commentId);
    }

    public void deleteCommentTree(String commentId) {   // for cascades: silently fine if already gone
        if (comments.existsById(commentId)) {
            deleteTree(commentId);
        }
    }

    private void deleteTree(String commentId) {
        for (Comment reply : comments.findByParentCommentId(commentId)) {
            deleteTree(reply.getId());
        }
        likes.deleteByCommentId(commentId);
        comments.deleteById(commentId);
    }
}
