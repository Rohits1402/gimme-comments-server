package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.LoginRequest;
import io.github.rohits1402.gimmecomments.dto.RegisterRequest;
import io.github.rohits1402.gimmecomments.dto.UserResponse;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/v1/auth")
public class AuthController {
    private final UserService userService;

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

}
