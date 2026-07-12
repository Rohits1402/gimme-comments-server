package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.Like;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LikeRepository extends MongoRepository<Like, String> {

    List<Like> findByCommentId(String commentId);

    long countByCommentId(String commentId);

    boolean existsByUserIdAndCommentId(String userId, String commentId);

    List<Like> findByUserId(String userId);
}
