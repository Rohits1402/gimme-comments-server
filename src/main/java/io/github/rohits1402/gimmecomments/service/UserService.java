package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.config.RateLimiter;
import io.github.rohits1402.gimmecomments.exception.*;
import io.github.rohits1402.gimmecomments.model.Gender;
import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.*;

@Service
public class UserService {

    private final UserRepository users;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final FileStorageService fileStorageService;
    private final RateLimiter rateLimiter;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository users,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       FileStorageService fileStorageService, RateLimiter rateLimiter, ApplicationEventPublisher events) {
        this.users = users;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.fileStorageService = fileStorageService;
        this.rateLimiter = rateLimiter;
        this.events = events;
    }

    // ---- the one place where a String id becomes a UUID ----------------

    private static UUID toUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("User not found");
        }
    }

    // ---- accounts ------------------------------------------------------

    @Transactional
    public User register(String name, String email, String password) {
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (!ConstraintViolations.isViolationOf(e, ConstraintViolations.USER_EMAIL)) {
                throw e;
            }
            throw new ConflictException("Email already registered");
        }
    }

    public String login(String email, String password) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new UnauthenticatedException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthenticatedException("Invalid Credentials");
        }
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Email is not verified");
        }
        if (!user.isAccountActive()) {
            throw new ForbiddenException("Account is deactivated (Contact administrator)");
        }

        return jwtService.generateToken(user.getId().toString());
    }

    @Transactional
    public void deleteUser(String userId) {
        UUID id = toUuid(userId);
        if (!users.existsById(id)) {
            throw new NotFoundException("User not found");
        }
        users.deleteById(id);          // websites, comments and likes cascade in the database
    }

    public User getById(String id) {
        return users.findById(toUuid(id))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    // ---- OTP flows -----------------------------------------------------

    private static final long CODES_PER_EMAIL = 3;
    private static final Duration CODE_WINDOW = Duration.ofMinutes(10);

    public void sendVerificationOtp(String email) {
        requireCodeAllowance(email);
        users.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                otpService.generate(email, OtpPurpose.ACCOUNT_VERIFICATION);
            }
        });
    }

    public void sendPasswordResetOtp(String email) {
        requireCodeAllowance(email);
        users.findByEmail(email)
                .ifPresent(user -> otpService.generate(email, OtpPurpose.PASSWORD_RESET));
    }

    private void requireCodeAllowance(String email) {
        String key = "otp:" + email.trim().toLowerCase(Locale.ROOT);
        if (!rateLimiter.allow(key, CODES_PER_EMAIL, CODE_WINDOW)) {
            throw new TooManyRequestsException(
                    "Too many codes requested for that address. Wait a few minutes and try again.",
                    CODE_WINDOW);
        }
    }

    @Transactional
    public void verifyAccount(String email, String otp) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("OTP is invalid"));
        otpService.verifyAndConsume(email, otp, OtpPurpose.ACCOUNT_VERIFICATION);
        user.setEmailVerified(true);
    }


    public void verifyResetOtp(String email, String otp) {
        otpService.verify(email, otp, OtpPurpose.PASSWORD_RESET);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("OTP is invalid"));
        otpService.verifyAndConsume(email, otp, OtpPurpose.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    // ---- profile -------------------------------------------------------

    @Transactional
    public User updateProfileImage(String userId, MultipartFile file) {
        User user = getById(userId);
        String oldUrl = user.getProfileImage();

        // The upload stays inside the transaction, because the row needs the URL it
        // returns. If the commit then fails, the new file is orphaned: wasted bytes,
        // and nothing pointing at them.
        user.setProfileImage(fileStorageService.store(file));

        // Delete must not stay. Here it destroyed the old file before the row that
        // replaced it existed, so a rollback left a reference to nothing.
        if (oldUrl != null && !oldUrl.isBlank()) {
            events.publishEvent(new ObsoleteImage(oldUrl));
        }
        return user;
    }

    @Transactional
    public User updateProfile(String userId, String name, Gender gender, String birthday) {
        if ((name == null || name.isEmpty()) && gender == null && (birthday == null || birthday.isEmpty())) {
            throw new BadRequestException("Please provide fields to update");
        }

        User user = getById(userId);
        if (name != null && !name.isEmpty()) user.setName(name);
        if (gender != null) user.setGender(gender);
        if (birthday != null && !birthday.isEmpty()) user.setBirthday(birthday);
        return user;
    }

    @Transactional
    public User updatePassword(String userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Wrong Old Password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return user;
    }

    @Transactional
    public void deleteProfile(String userId) {
        deleteUser(userId);
    }
}