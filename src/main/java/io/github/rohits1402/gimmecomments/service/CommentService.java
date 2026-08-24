package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository comments;
    private final UserRepository users;
    private final WebsiteRepository websites;
    private final WebsiteService websiteService;

    public CommentService(CommentRepository comments,
                          UserRepository users,
                          WebsiteRepository websites,
                          WebsiteService websiteService) {
        this.comments = comments;
        this.users = users;
        this.websites = websites;
        this.websiteService = websiteService;
    }

    private static UUID toUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Comment not found");
        }
    }

    private static UUID toUuidOrNull(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional
    public void deleteComment(String callerUserId, String commentId) {
        getOwned(callerUserId, commentId);
        comments.deleteById(toUuid(commentId));   // replies and likes cascade in the database
    }

    @Transactional
    public Comment create(String userId, String websiteId, String description, String parentCommentId) {
        websiteService.requireExists(websiteId);

        Comment comment = new Comment();
        comment.setWebsite(websites.getReferenceById(UUID.fromString(websiteId)));
        comment.setAuthor(users.getReferenceById(UUID.fromString(userId)));
        comment.setCommentDescription(description);

        if (parentCommentId != null) {
            UUID parentId = toUuidOrNull(parentCommentId);
            if (parentId == null || !comments.existsById(parentId)) {
                throw new BadRequestException("No parent comment found with id : " + parentCommentId);
            }
            comment.setParent(comments.getReferenceById(parentId));
        }

        return comments.save(comment);
    }

    public Comment getOwned(String callerUserId, String id) {
        return comments.findById(toUuid(id))
                .filter(c -> c.getAuthor() != null
                        && c.getAuthor().getId().toString().equals(callerUserId))
                .orElseThrow(() -> new NotFoundException("Comment not found"));
    }

    public List<Comment> getAllForWebsite(String websiteId) {
        UUID id = toUuidOrNull(websiteId);
        return id == null ? List.of() : comments.findByWebsiteIdWithAuthors(id);
    }

    @Transactional
    public Comment update(String callerUserId, String commentId, String description) {
        Comment comment = getOwned(callerUserId, commentId);
        comment.setCommentDescription(description);
        return comment;
    }
}