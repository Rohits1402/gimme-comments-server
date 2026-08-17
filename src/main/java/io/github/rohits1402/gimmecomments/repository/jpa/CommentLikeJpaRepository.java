package io.github.rohits1402.gimmecomments.repository.jpa;

import io.github.rohits1402.gimmecomments.model.jpa.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentLikeJpaRepository extends JpaRepository<CommentLike, UUID> {

    long countByCommentId(UUID commentId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);
}