package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.repository.*;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {
    /**
     * One page of threads, plus the marker for the page after it.
     */
    public record CommentPage(List<CommentWithLikes> comments, String nextCursor, long total) {
    }

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
            Comment parent = parentId == null ? null : comments.findById(parentId).orElse(null);
            if (parent == null) {
                throw new BadRequestException("No parent comment found with id : " + parentCommentId);
            }
            // The thread is worked out by Comment#joinThread when the row is saved.
            comment.setParent(parent);
        }

        return comments.save(comment);
    }

    public Comment getOwned(String callerUserId, String id) {
        return comments.findById(toUuid(id))
                .filter(c -> c.getAuthor() != null
                        && c.getAuthor().getId().toString().equals(callerUserId))
                .orElseThrow(() -> new NotFoundException("Comment not found"));
    }

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    public CommentPage getPageForWebsite(String websiteId, String callerUserId,
                                         String cursor, Integer requestedSize) {
        UUID id = toUuidOrNull(websiteId);
        if (id == null) {
            return new CommentPage(List.of(), null, 0);
        }

        // A caller does not get to decide how much work we do.
        int size = requestedSize == null ? DEFAULT_PAGE_SIZE
                : Math.clamp(requestedSize, 1, MAX_PAGE_SIZE);

        // Ask for one more than we will send. If it comes back, there is another
        // page — answered without a second COUNT query.
        List<Comment> roots = cursor == null || cursor.isBlank()
                ? comments.findRoots(id, Limit.of(size + 1))
                : decodeAndFind(id, cursor, size + 1);

        String nextCursor = null;
        if (roots.size() > size) {
            roots = roots.subList(0, size);
            Comment last = roots.getLast();
            nextCursor = new CommentCursor(last.getCreatedAt(), last.getId()).encode();
        }

        long total = comments.countByWebsiteId(id);
        if (roots.isEmpty()) {
            return new CommentPage(List.of(), null, total);
        }

        // The threads themselves, then everything hanging off them. Two queries,
        // whatever the depth.
        List<Comment> page = new ArrayList<>(roots);
        page.addAll(comments.findRepliesForRoots(roots.stream().map(Comment::getId).toList()));

        return new CommentPage(withLikes(page, callerUserId), nextCursor, total);
    }

    private List<Comment> decodeAndFind(UUID websiteId, String cursor, int limit) {
        CommentCursor from = CommentCursor.decode(cursor);
        return comments.findRootsBefore(websiteId, from.createdAt(), from.id(), Limit.of(limit));
    }

    private List<CommentWithLikes> withLikes(List<Comment> page, String callerUserId) {
        List<UUID> commentIds = page.stream().map(Comment::getId).toList();

        Map<UUID, Long> counts = likes.countByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(LikeCount::commentId, LikeCount::total));

        UUID callerId = callerUserId == null ? null : toUuidOrNull(callerUserId);
        Set<UUID> mine = callerId == null
                ? Set.of()
                : Set.copyOf(likes.findLikedCommentIds(callerId, commentIds));

        return page.stream()
                .map(c -> new CommentWithLikes(c, counts.getOrDefault(c.getId(), 0L), mine.contains(c.getId())))
                .toList();
    }

    @Transactional
    public Comment update(String callerUserId, String commentId, String description) {
        Comment comment = getOwned(callerUserId, commentId);
        comment.setCommentDescription(description);
        return comment;
    }
}