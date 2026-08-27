package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.config.RateLimiter;
import io.github.rohits1402.gimmecomments.config.SecurityConfig;
import io.github.rohits1402.gimmecomments.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every site that embeds the widget calls this endpoint first, and the loader gives
 * up if it does not answer. It is the single point where the whole product fails at
 * once, so it is worth a test of its own.
 * <p>
 * The case that broke it: the widget build inlines its CSS into the JavaScript, so
 * the css directory it used to scan no longer exists at all.
 */
@WebMvcTest(InitializationController.class)
@Import({SecurityConfig.class, RateLimiter.class})
class InitializationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void answersAnonymously_evenWhenThereIsNoCssDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/initialization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsFiles").isArray())
                .andExpect(jsonPath("$.cssFiles").isArray());
    }

    @Test
    void listsTheWidgetBundle() throws Exception {
        mockMvc.perform(get("/api/v1/initialization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsFiles[0]").value(org.hamcrest.Matchers.endsWith(".js")));
    }
}
