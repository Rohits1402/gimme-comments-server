package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.CreateWebsiteRequest;
import io.github.rohits1402.gimmecomments.dto.UpdateWebsiteRequest;
import io.github.rohits1402.gimmecomments.dto.WebsiteResponse;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.service.WebsiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/websites")
public class WebsiteController {
    private final WebsiteService websiteService;

    public WebsiteController(WebsiteService websiteService) {
        this.websiteService = websiteService;
    }

    record WebsiteListEnvelope(List<WebsiteResponse> websites) {
    }

    record WebsiteEnvelope(WebsiteResponse website) {
    }

    record WebsiteMsgEnvelope(String msg, WebsiteResponse website) {
    }

    record MsgEnvelope(String msg) {
    }

    @GetMapping
    public WebsiteListEnvelope getAll(@RequestParam String userId) {   // TODO Week 3: from token
        List<WebsiteResponse> list = websiteService.getAllByUser(userId).stream()
                .map(WebsiteResponse::from)
                .toList();
        return new WebsiteListEnvelope(list);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WebsiteMsgEnvelope create(@Valid @RequestBody CreateWebsiteRequest request) {
        Website created = websiteService.create(request.userId(), request.websiteName(),
                request.websiteDescription(), request.websiteUrl(), request.websiteConfiguration());
        return new WebsiteMsgEnvelope("Website created successfully", WebsiteResponse.from(created));
    }

    @GetMapping("/{id}")
    public WebsiteEnvelope getOne(@PathVariable String id) {
        return new WebsiteEnvelope(WebsiteResponse.from(websiteService.getById(id)));
    }

    @GetMapping("/exists/{id}")
    public MsgEnvelope exists(@PathVariable String id) {
        websiteService.getById(id);           // throws NotFoundException if absent — that is the whole check
        return new MsgEnvelope("Website found with id : " + id);
    }

    @PatchMapping("/{id}")
    public WebsiteMsgEnvelope update(@PathVariable String id, @RequestBody UpdateWebsiteRequest request) {
        Website updated = websiteService.update(id, request.websiteName(),
                request.websiteDescription(), request.websiteConfiguration());
        return new WebsiteMsgEnvelope("Website updated successfully", WebsiteResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public MsgEnvelope delete(@PathVariable String id) {
        websiteService.deleteWebsite(id);
        return new MsgEnvelope("Website Profile deleted");
    }
}
