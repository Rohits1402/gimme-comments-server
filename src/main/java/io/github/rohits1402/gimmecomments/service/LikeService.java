package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.CommentLike;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.CommentLikeRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LikeService {

    private final CommentLikeRepository likes;
    private final CommentRepository comments;
    private final UserRepository users;

    public LikeService(CommentLikeRepository likes,
                       CommentRepository comments,
                       UserRepository users) {
        this.likes = likes;
        this.comments = comments;
        this.users = users;
    }

    private static UUID toUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Comment not found");
        }
    }

    @Transactional
    public CommentLike create(String userId, String commentId) {
        UUID commentUuid = toUuid(commentId);
        UUID userUuid = UUID.fromString(userId);

        if (!comments.existsById(commentUuid)) {
            throw new NotFoundException("Comment not found");
        }
        if (likes.existsByCommentIdAndUserId(commentUuid, userUuid)) {
            throw new BadRequestException("User has already liked comment with id : " + commentId);
        }

        CommentLike like = new CommentLike();
        like.setComment(comments.getReferenceById(commentUuid));
        like.setUser(users.getReferenceById(userUuid));
        return likes.save(like);
    }

    @Transactional
    public void delete(String userId, String commentId) {
        UUID commentUuid = toUuid(commentId);
        UUID userUuid = UUID.fromString(userId);

        if (!comments.existsById(commentUuid)) {
            throw new NotFoundException("Comment not found");
        }

        CommentLike like = likes.findByCommentIdAndUserId(commentUuid, userUuid)
                .orElseThrow(() -> new BadRequestException("User has not liked comment with id : " + commentId));

        likes.delete(like);
    }
}