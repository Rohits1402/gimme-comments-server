package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.website.id = :websiteId")
    List<Comment> findByWebsiteIdWithAuthors(@Param("websiteId") UUID websiteId);

    boolean existsByWebsiteId(UUID websiteId);

    List<Comment> findByWebsiteIdAndParentIsNull(UUID websiteId);
}