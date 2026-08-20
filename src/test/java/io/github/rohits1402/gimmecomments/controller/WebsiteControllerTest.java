package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.config.SecurityConfig;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.jpa.User;
import io.github.rohits1402.gimmecomments.model.jpa.Website;
import io.github.rohits1402.gimmecomments.service.JwtService;
import io.github.rohits1402.gimmecomments.service.WebsiteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebsiteController.class)
@Import(SecurityConfig.class)
class WebsiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebsiteService websiteService;

    @MockitoBean
    private JwtService jwtService;

    private static UsernamePasswordAuthenticationToken callerIs(String userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    void listWebsites_returns403_whenNoTokenPresent() throws Exception {
        mockMvc.perform(get("/api/v1/websites"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listWebsites_returns200_withSnakeCaseEnvelope() throws Exception {
        User owner = new User();
        owner.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        Website site = new Website();
        site.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        site.setOwner(owner);
        site.setName("My Blog");
        site.setUrl("https://blog.example.com");

        when(websiteService.getAllByUser("user123")).thenReturn(List.of(site));

        mockMvc.perform(get("/api/v1/websites").with(authentication(callerIs("user123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.websites").isArray())
                .andExpect(jsonPath("$.websites[0].by_user").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.websites[0].website_name").value("My Blog"));
    }

    @Test
    void createWebsite_takesIdentityFromTheToken_notTheRequestBody() throws Exception {
        User creator = new User();
        creator.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        Website created = new Website();
        created.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        created.setOwner(creator);
        created.setName("My Blog");

        when(websiteService.create(eq("22222222-2222-2222-2222-222222222222"), any(), any(), any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/websites")
                        .with(authentication(callerIs("22222222-2222-2222-2222-222222222222")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "website_name": "My Blog",
                                  "website_description": "Things I write",
                                  "website_url": "https://blog.example.com",
                                  "by_user": "attacker999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").value("Website created successfully"))
                .andExpect(jsonPath("$.website.by_user").value("22222222-2222-2222-2222-222222222222"));

        verify(websiteService).create(eq("22222222-2222-2222-2222-222222222222"), eq("My Blog"), any(), any(), any());
    }

    @Test
    void getOne_returns404_whenTheWebsiteBelongsToSomeoneElse() throws Exception {
        when(websiteService.getOwned("site999", "user123"))
                .thenThrow(new NotFoundException("Website with given id not found"));

        mockMvc.perform(get("/api/v1/websites/site999").with(authentication(callerIs("user123"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value("Website with given id not found"));
    }
}