package io.github.rohits1402.gimmecomments.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces =     MediaType.TEXT_HTML_VALUE)
    public String home() {
        return "<h1>Welcome to <b>Gimme Comments App</b>!</h1>"
                + "<h3><a href=\"https://documenter.getpostman.com/view/15926122/2s93RMVb6i\">Read Documentation</a></h3>";
    }
}