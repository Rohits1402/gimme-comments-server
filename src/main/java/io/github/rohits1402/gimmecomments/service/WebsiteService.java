package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.ConflictException;
import io.github.rohits1402.gimmecomments.exception.ConstraintViolations;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.repository.CommentCount;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WebsiteService {

    private final WebsiteRepository websites;
    private final UserRepository users;
    private final CommentRepository comments;

    public WebsiteService(WebsiteRepository websites, UserRepository users,
                          CommentRepository comments) {
        this.websites = websites;
        this.users = users;
        this.comments = comments;
    }

    private static UUID toUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Website with given id not found");
        }
    }

    private static UUID toUuidOrNull(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void requireExists(String websiteId) {
        if (!websites.existsById(toUuid(websiteId))) {
            throw new NotFoundException("Website with given id not found");
        }
    }

    public Website getOwned(String id, String callerUserId) {
        return websites.findById(toUuid(id))
                .filter(w -> w.getOwner().getId().toString().equals(callerUserId))
                .orElseThrow(() -> new NotFoundException("Website with given id not found"));
    }

    @Transactional
    public Website create(String userId, String websiteName, String websiteDescription,
                          String websiteUrl, Map<String, Object> websiteConfiguration) {

        if (websites.existsByUrl(websiteUrl)) {
            throw new ConflictException("Website already exist!");
        }

        Website website = new Website();
        website.setOwner(users.getReferenceById(UUID.fromString(userId)));
        website.setName(websiteName);
        website.setUrl(websiteUrl);
        if (websiteDescription != null) website.setDescription(websiteDescription);
        if (websiteConfiguration != null) website.setWebsiteConfiguration(websiteConfiguration);

        try {
            return websites.saveAndFlush(website);
        } catch (DataIntegrityViolationException e) {
            if (!ConstraintViolations.isViolationOf(e, ConstraintViolations.WEBSITE_URL)) {
                throw e;
            }
            throw new ConflictException("Email already registered");
        }
    }

    /**
     * The caller's websites, each carrying how many comments it has. Two queries in
     * total: the websites, then one grouped count. Asking per website would be the
     * same N+1 the comment list already avoids, just one level up.
     */
    public List<WebsiteWithCount> getAllByUser(String userId) {
        UUID ownerId = toUuidOrNull(userId);
        if (ownerId == null) {
            return List.of();
        }

        List<Website> found = websites.findByOwnerId(ownerId);
        if (found.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> counts = comments
                .countByWebsiteIds(found.stream().map(Website::getId).toList())
                .stream()
                .collect(Collectors.toMap(CommentCount::websiteId, CommentCount::total));

        return found.stream()
                .map(w -> new WebsiteWithCount(w, counts.getOrDefault(w.getId(), 0L)))
                .toList();
    }

    public Website getById(String id) {
        return websites.findById(toUuid(id))
                .orElseThrow(() -> new NotFoundException("Website with given id not found"));
    }

    @Transactional
    public Website update(String id, String callerId, String websiteName, String websiteDescription,
                          Map<String, Object> websiteConfiguration) {

        Website website = getOwned(id, callerId);
        if (websiteName != null) website.setName(websiteName);
        if (websiteDescription != null) website.setDescription(websiteDescription);
        if (websiteConfiguration != null) website.setWebsiteConfiguration(websiteConfiguration);
        return website;
    }

    @Transactional
    public void deleteWebsite(String websiteId, String callerUserId) {
        getOwned(websiteId, callerUserId);
        websites.deleteById(toUuid(websiteId));
    }
}