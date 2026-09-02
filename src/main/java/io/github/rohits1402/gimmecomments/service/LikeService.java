package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.exception.ConstraintViolations;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.CommentLike;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.CommentLikeRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
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


        try {
            // saveAndFlush, not save: save only tells the session about the row, and the
            // INSERT would then be sent at commit - outside this method, where no catch
            // block of ours can reach it.
            return likes.saveAndFlush(like);
        } catch (DataIntegrityViolationException e) {
            if (!ConstraintViolations.isViolationOf(e, ConstraintViolations.ONE_LIKE_PER_USER)) {
                throw e;
            }
            // Somebody inserted the same like between our check and our insert. It is
            // the same situation the check found, so it has to read the same way.
            throw new BadRequestException("User has already liked comment with id : " + commentId);
        }
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