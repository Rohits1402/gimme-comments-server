package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.config.SecurityConfig;
import io.github.rohits1402.gimmecomments.exception.NotFoundException;
import io.github.rohits1402.gimmecomments.model.Website;
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
        Website site = new Website();
        site.setId("site1");
        site.setUserId("user123");
        site.setWebsiteName("My Blog");
        site.setWebsiteUrl("https://blog.example.com");

        when(websiteService.getAllByUser("user123")).thenReturn(List.of(site));

        mockMvc.perform(get("/api/v1/websites").with(authentication(callerIs("user123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.websites").isArray())
                .andExpect(jsonPath("$.websites[0].by_user").value("user123"))
                .andExpect(jsonPath("$.websites[0].website_name").value("My Blog"));
    }

    @Test
    void createWebsite_takesIdentityFromTheToken_notTheRequestBody() throws Exception {
        Website created = new Website();
        created.setId("site1");
        created.setUserId("user123");
        created.setWebsiteName("My Blog");

        when(websiteService.create(eq("user123"), any(), any(), any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/websites")
                        .with(authentication(callerIs("user123")))
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
                .andExpect(jsonPath("$.website.by_user").value("user123"));

        verify(websiteService).create(eq("user123"), eq("My Blog"), any(), any(), any());
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