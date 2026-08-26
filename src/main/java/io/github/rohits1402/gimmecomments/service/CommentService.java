package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository comments;
    private final UserRepository users;
    private final WebsiteRepository websites;
    private final WebsiteService websiteService;
    private final CommentLikeRepository likes;

    public CommentService(CommentRepository comments,
                          UserRepository users,
                          WebsiteRepository websites,
                          WebsiteService websiteService, CommentLikeRepository likes) {
        this.comments = comments;
        this.users = users;
        this.websites = websites;
        this.websiteService = websiteService;
        this.likes = likes;
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

    /**
     * Two people may delete a comment: whoever wrote it, and whoever owns the website
     * it sits on. The second is moderation — a comments product where the site owner
     * cannot remove abuse from their own page is not finished.
     * <p>
     * Editing stays author-only on purpose. Removing someone's words is moderation;
     * rewriting them and leaving their name on top is something else.
     */
    @Transactional
    public void deleteComment(String callerUserId, String commentId) {
        Comment comment = comments.findById(toUuid(commentId))
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        boolean wroteIt = comment.getAuthor() != null
                && comment.getAuthor().getId().toString().equals(callerUserId);
        // One extra query: the website is a proxy, so reaching its owner loads it.
        boolean ownsTheSite = comment.getWebsite().getOwner().getId().toString()
                .equals(callerUserId);

        if (!wroteIt && !ownsTheSite) {
            // 404 rather than 403, matching deviation #4: refuse to confirm that
            // someone else's comment exists.
            throw new NotFoundException("Comment not found");
        }

        comments.deleteById(comment.getId());   // replies and likes cascade in the database
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

    @Transactional(readOnly = true)
    public List<CommentWithLikes> getAllForWebsite(String websiteId, String callerUserId) {
        UUID id = toUuidOrNull(websiteId);
        if (id == null) {
            return List.of();
        }

        List<Comment> found = comments.findByWebsiteIdWithAuthors(id);
        if (found.isEmpty()) {
            return List.of();
        }

        List<UUID> commentIds = found.stream().map(Comment::getId).toList();

        Map<UUID, Long> counts = likes.countByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(LikeCount::commentId, LikeCount::total));

        UUID callerId = callerUserId == null ? null : toUuidOrNull(callerUserId);
        Set<UUID> mine = callerId == null
                ? Set.of()
                : Set.copyOf(likes.findLikedCommentIds(callerId, commentIds));

        return found.stream()
                .map(c -> new CommentWithLikes(c,
                        counts.getOrDefault(c.getId(), 0L),
                        mine.contains(c.getId())))
                .toList();
    }

    @Transactional
    public Comment update(String callerUserId, String commentId, String description) {
        Comment comment = getOwned(callerUserId, commentId);
        comment.setCommentDescription(description);
        return comment;
    }
}