package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.*;
import io.github.rohits1402.gimmecomments.model.Comment;
import io.github.rohits1402.gimmecomments.model.OtpPurpose;
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
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public UserService(UserRepository users, LikeRepository likes, CommentRepository comments, WebsiteRepository websites, WebsiteService websiteService, CommentService commentService, JwtService jwtService, PasswordEncoder passwordEncoder, OtpService otpService) {
        this.users = users;
        this.likes = likes;
        this.comments = comments;
        this.websites = websites;
        this.websiteService = websiteService;
        this.commentService = commentService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
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

    public String login(String email, String password) {
        User user = users.findByEmail(email).orElseThrow(() -> new UnauthenticatedException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthenticatedException("Invalid Credentials");
        }
        if (!user.isEmailVerified())
            throw new ForbiddenException("Email is not verified");

        if (!user.isAccountActive())
            throw new ForbiddenException("Account is deactivated (Contact administrator)");

        return jwtService.generateToken(user.getId());
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

    public User getById(String id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public void sendVerificationOtp(String email) {
        users.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                otpService.generate(email, OtpPurpose.ACCOUNT_VERIFICATION);
            }
        });
    }

    public void verifyAccount(String email, String otp) {
        User user = users.findByEmail(email).orElseThrow(() -> new BadRequestException("OTP is invalid"));
        otpService.verifyAndConsume(email, otp, OtpPurpose.ACCOUNT_VERIFICATION);
        user.setEmailVerified(true);
        users.save(user);
    }

    public void sendPasswordResetOtp(String email) {
        users.findByEmail(email).ifPresent(user -> {
            otpService.generate(email, OtpPurpose.PASSWORD_RESET);
        });
    }

    public void verifyResetOtp(String email, String otp) {
        otpService.verify(email, otp, OtpPurpose.PASSWORD_RESET);
    }

    public void resetPassword(String email, String otp, String newPassword) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("OTP is invalid"));
        otpService.verifyAndConsume(email, otp, OtpPurpose.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);
    }
}
