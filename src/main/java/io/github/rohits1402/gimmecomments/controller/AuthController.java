package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.*;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/v1/auth")
public class AuthController {
    private final UserService userService;

    record MsgEnvelope(String msg) {
    }

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User saved = userService.register(request.name(), request.email(), request.password());
        return UserResponse.from(saved);
    }

    record TokenEnvelope(String token) {
    }

    @PostMapping("/login")
    public TokenEnvelope login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.email(), request.password());
        return new TokenEnvelope(token);
    }

    @PostMapping("/account-verification/generate-otp")
    public MsgEnvelope generateVerificationOtp(@Valid @RequestBody GenerateOtpRequest request) {
        userService.sendVerificationOtp(request.email());
        return new MsgEnvelope("If an account exists, an OTP has been sent to the email");
    }

    @PostMapping("/account-verification/verify-account")
    public MsgEnvelope verifyAccount(@Valid @RequestBody OtpRequest request) {
        userService.verifyAccount(request.email(), request.otp());
        return new MsgEnvelope("Email verified successfully");
    }

    @PostMapping("/forget-password/generate-otp")
    public MsgEnvelope generateResetOtp(@Valid @RequestBody GenerateOtpRequest request) {
        userService.sendPasswordResetOtp(request.email());
        return new MsgEnvelope("If an account exists, an OTP has been sent to the email");
    }

    @PostMapping("/forget-password/verify-otp")
    public MsgEnvelope verifyResetOtp(@Valid @RequestBody OtpRequest request) {
        userService.verifyResetOtp(request.email(), request.otp());
        return new MsgEnvelope("OTP verified");
    }

    @PatchMapping("/forget-password/change-password")
    public MsgEnvelope changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.resetPassword(request.email(), request.otp(), request.newPassword());
        return new MsgEnvelope("Password updated");
    }
}
