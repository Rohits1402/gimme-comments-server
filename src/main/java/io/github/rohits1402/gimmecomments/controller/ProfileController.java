package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.UserResponse;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth/profile")
public class ProfileController {
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    record UserMsgEnvelope(String msg, UserResponse use) {
    }

    @PatchMapping("/update-profile-image")
    public UserMsgEnvelope updateProfileImage(@AuthenticationPrincipal String userId,
                                              @RequestParam("profile_image") MultipartFile file) {
        User user = userService.updateProfileImage(userId, file);
        return new UserMsgEnvelope("User Profile Image Updated", UserResponse.from(user));

    }
}

