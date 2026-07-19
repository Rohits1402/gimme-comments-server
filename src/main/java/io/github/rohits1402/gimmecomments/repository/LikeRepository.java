package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.Like;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends MongoRepository<Like, String> {

    List<Like> findByCommentId(String commentId);

    long countByCommentId(String commentId);

    boolean existsByUserIdAndCommentId(String userId, String commentId);

    Optional<Like> findByUserIdAndCommentId(String userId, String commentId);

    void deleteByCommentId(String commentId);

    void deleteByCommentIdIn(Collection<String> commentIds);

    void deleteByUserId(String userId);

    List<Like> findByUserId(String userId);
}
