package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.RegisterRequest;
import io.github.rohits1402.gimmecomments.dto.UserResponse;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("api/v1/auth")
public class AuthController {
    private final UserRepository users;

    public AuthController(UserRepository users) {
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("msg", "Email already registered"));
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(request.password());
        User saved = users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

}
