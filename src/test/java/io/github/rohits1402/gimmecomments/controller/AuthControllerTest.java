package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.config.RateLimiter;
import io.github.rohits1402.gimmecomments.config.SecurityConfig;
import io.github.rohits1402.gimmecomments.exception.ConflictException;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.service.AuthTokens;
import io.github.rohits1402.gimmecomments.service.JwtService;
import io.github.rohits1402.gimmecomments.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RateLimiter.class})
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtService jwtService;

    /**
     * These three are reachable without a token, and they have to be. Refreshing and
     * signing out are exactly what a caller does when their access token has expired,
     * so requiring one would make them unusable at the moment they are needed.
     * <p>
     * Worth a test because the service-level tests cannot see it: they call
     * UserService directly and never pass through the security filter chain, so a
     * missing permitAll entry looks perfectly healthy from down there. It was found
     * by hand, as a 403 from an endpoint that had already been written.
     */
    @Test
    void refreshAndLogoutAreReachableWithoutAToken() throws Exception {
        when(userService.refresh(anyString()))
                .thenReturn(new AuthTokens("new-access", "new-refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token": "whatever"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access"))
                .andExpect(jsonPath("$.refresh_token").value("new-refresh"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refresh_token": "whatever"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void register_returns201_andNeverLeaksThePassword() throws Exception {
        User saved = new User();
        saved.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        saved.setName("Rohit");
        saved.setEmail("rohit@example.com");
        saved.setPassword("$2a$10$averyrealbcrypthash");

        when(userService.register(anyString(), anyString(), anyString())).thenReturn(saved);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rohit",
                                  "email": "rohit@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.email").value("rohit@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_returns400_withEveryValidationMessage_sorted() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "name": "",
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value(
                        "Password must be at least 8 characters, Please provide name, Please provide valid email"));
    }


    @Test
    void register_returns409_whenEmailAlreadyRegistered() throws Exception {
        when(userService.register(anyString(), anyString(), anyString()))
                .thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rohit",
                                  "email": "taken@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.msg").value("Email already registered"));
    }

}