package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

    long countByCommentId(UUID commentId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);
}