package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.ConflictException;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.repository.CommentRepository;
import io.github.rohits1402.gimmecomments.repository.LikeRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import io.github.rohits1402.gimmecomments.repository.WebsiteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository users;
    private final LikeRepository likes;
    private final CommentRepository comments;
    private final WebsiteRepository websites;
    private final WebsiteService websiteService;
    private final CommentService commentService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, LikeRepository likes, CommentRepository comments, WebsiteRepository websites, WebsiteService websiteService, CommentService commentService, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.likes = likes;
        this.comments = comments;
        this.websites = websites;
        this.websiteService = websiteService;
        this.commentService = commentService;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String name, String email, String password) {
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        return users.save(user);
    }

    public void deleteUser(String userId) {
        if (!users.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        likes.deleteByUserId(userId);                              // 1. likes they gave
        for (Website site : websites.findByUserId(userId)) {
            websiteService.purgeWebsite(site.getId());             // 2. their websites + all contents
        }
        for (Comment comment : comments.findByUserId(userId)) {
            commentService.deleteCommentTree(comment.getId());     // 3. their comments elsewhere (may
        }                                                          //    already be gone — idempotent)
        users.deleteById(userId);                                  // 4. the user record, LAST
    }
}
