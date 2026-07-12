package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByWebsiteId(String websiteId);

    List<Comment> findByParentCommentId(String parentCommentId);

    List<Comment> findByUserId(String userId);

    // derived, one new word: IsNull — the root comments of a website (no parent)
    List<Comment> findByWebsiteIdAndParentCommentIdIsNull(String websiteId);

    // explicit query: raw Mongo JSON, ?0 and ?1 are the method arguments in order
    @Query("{ 'websiteId': ?0, 'commentDescription': { $regex: ?1, $options: 'i' } }")
    List<Comment> searchByText(String websiteId, String text);
}
