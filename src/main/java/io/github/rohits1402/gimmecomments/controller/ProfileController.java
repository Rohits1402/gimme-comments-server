package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.UpdatePasswordRequest;
import io.github.rohits1402.gimmecomments.dto.UpdateProfileRequest;
import io.github.rohits1402.gimmecomments.dto.UserResponse;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth/profile")
public class ProfileController {
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    record MsgEnvelope(String msg) {
    }

    record UserMsgEnvelope(String msg, UserResponse user) {
    }

    record UserEnvelope(UserResponse user) {
    }

    @PatchMapping("/update-profile-image")
    public UserMsgEnvelope updateProfileImage(@AuthenticationPrincipal String userId,
                                              @RequestParam("profile_image") MultipartFile file) {
        User user = userService.updateProfileImage(userId, file);
        return new UserMsgEnvelope("User Profile Image Updated", UserResponse.from(user));

    }

    @GetMapping
    public UserEnvelope getProfile(@AuthenticationPrincipal String userId) {
        User user = userService.getById(userId);
        return new UserEnvelope(UserResponse.from(user));
    }

    @PatchMapping("/update-profile")
    public UserMsgEnvelope updateProfile(@AuthenticationPrincipal String userId, @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request.name(), request.gender(), request.birthday());
        return new UserMsgEnvelope("User Profile Updated", UserResponse.from(user));
    }


    @PatchMapping("/update-password")
    public MsgEnvelope updatePassword(@AuthenticationPrincipal String userId, @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request.oldPassword(), request.newPassword());
        return new MsgEnvelope("Password Updated");
    }

    @DeleteMapping("/delete-profile")
    public MsgEnvelope deleteProfile(@AuthenticationPrincipal String userId) {
        userService.deleteProfile(userId);
        return new MsgEnvelope("User Profile Deleted");
    }
}

