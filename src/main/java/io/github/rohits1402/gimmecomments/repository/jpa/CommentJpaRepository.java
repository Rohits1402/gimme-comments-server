package io.github.rohits1402.gimmecomments.repository.jpa;

import io.github.rohits1402.gimmecomments.model.jpa.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentJpaRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByWebsiteIdAndParentIsNull(UUID websiteId);
}