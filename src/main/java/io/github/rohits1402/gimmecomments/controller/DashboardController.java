package io.github.rohits1402.gimmecomments.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the dashboard, which is a single-page application. The browser can ask for
 * any of its routes directly — a bookmark, a refresh on /websites/{id} — and every
 * one of them has to be answered with the same index.html, leaving the app to read
 * the path for itself.
 * <p>
 * The routes are listed rather than caught with "/**" on purpose. A controller
 * mapping wins over Spring's static resource handling, so a catch-all here would
 * swallow /build/**, /uploads/** and the widget loader, and the widget would stop
 * loading on every site that embeds it.
 */
@Controller
public class DashboardController {

    @GetMapping({"/", "/sign-in", "/websites", "/websites/{id}", "/account"})
    public String dashboard() {
        return "forward:/app/index.html";
    }
}
