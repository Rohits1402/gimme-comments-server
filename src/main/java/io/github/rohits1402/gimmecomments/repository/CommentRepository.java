package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByWebsiteId(String websiteId);

    List<Comment> findByParentCommentId(String parentCommentId);

    List<Comment> findByUserId(String userId);
}
