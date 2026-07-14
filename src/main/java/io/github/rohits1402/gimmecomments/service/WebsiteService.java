package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.LikeRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebsiteService {
    private final WebsiteRepository websites;
    private final CommentRepository comments;
    private final LikeRepository likes;

    public WebsiteService(WebsiteRepository websites, CommentRepository comments, LikeRepository likes) {
        this.websites = websites;
        this.comments = comments;
        this.likes = likes;
    }

    public void deleteWebsite(String websiteId) {
        if (!websites.existsById(websiteId)) {
            throw new NotFoundException("Website not found");
        }
        purgeWebsite(websiteId);
    }

    public void purgeWebsite(String websiteId) {
        List<String> commentIds = comments.findByWebsiteId(websiteId).stream().map(Comment::getId).toList();

        likes.deleteByCommentIdIn(commentIds);
        comments.deleteByWebsiteId(websiteId);
        websites.deleteById(websiteId);

    }

}
