package io.github.rohits1402.gimmecomments.controller;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("api/v1/hello")
public class HelloController {

    @GetMapping("/greet/{name}")
    public GreetingResponse greet(@PathVariable String name, @RequestParam(defaultValue = "false") boolean shout) {
        String message = shout ? ("Hello" + name.toUpperCase() + "!!!!") : ("Hello " + name);
        return new GreetingResponse(message, Instant.now());
    }

    public record GreetingResponse(String message, Instant at) {

    }

}
