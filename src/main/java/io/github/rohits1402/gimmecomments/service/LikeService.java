package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.Like;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.LikeRepository;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final CommentRepository comments;

    public LikeService(LikeRepository likeRepository, CommentRepository comments) {
        this.likeRepository = likeRepository;
        this.comments = comments;
    }

    public Like create(String userId, String commentId) {
        if (!comments.existsById(commentId))
            throw new NotFoundException("Comment not found");
        if (likeRepository.existsByUserIdAndCommentId(userId, commentId))
            throw new BadRequestException("User has already liked comment with id : " + commentId);

        Like like = new Like();
        like.setUserId(userId);
        like.setCommentId(commentId);
        likeRepository.save(like);
        return like;
    }

    public void delete(String userId, String commentId) {
        if (!comments.existsById(commentId))
            throw new NotFoundException("Comment not found");

        Like like = likeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new BadRequestException("User has not liked comment with id : " + commentId));

        likeRepository.delete(like);

    }
}
