package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.ConflictException;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.LikeRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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

    public Website create(String userId, String websiteName, String websiteDescription, String websiteUrl, Map<String, Object> websiteConfiguration) {
        if (websites.existsByWebsiteUrl(websiteUrl)) throw new ConflictException("Website already exist!");

        Website website = new Website();
        website.setUserId(userId);
        website.setWebsiteName(websiteName);
        website.setWebsiteDescription(websiteDescription);
        website.setWebsiteUrl(websiteUrl);
        website.setWebsiteConfiguration(websiteConfiguration);

        return websites.save(website);
    }

    public List<Website> getAllByUser(String userId) {
        return websites.findByUserId(userId);
    }


    public Website getById(String id) {
        return websites.findById(id)
                .orElseThrow(() -> new NotFoundException("Website with given id not found"));
    }

    public Website update(String id, String websiteName, String websiteDescription,
                          Map<String, Object> websiteConfiguration) {
        Website website = getById(id);
        if (websiteName != null) website.setWebsiteName(websiteName);
        if (websiteDescription != null) website.setWebsiteDescription(websiteDescription);
        if (websiteConfiguration != null) website.setWebsiteConfiguration(websiteConfiguration);
        return websites.save(website);
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
