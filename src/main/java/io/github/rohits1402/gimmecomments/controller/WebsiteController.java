package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.CreateWebsiteRequest;
import io.github.rohits1402.gimmecomments.dto.UpdateWebsiteRequest;
import io.github.rohits1402.gimmecomments.dto.WebsiteResponse;
import io.github.rohits1402.gimmecomments.model.Website;
import io.github.rohits1402.gimmecomments.service.WebsiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Websites", description = "Register and manage the sites that embed the comment widget")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(summary = "List the caller's websites",
            description = "Returns only websites owned by the authenticated user. Never anyone else's.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The caller's websites"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid token", content = @Content)
    })
    @GetMapping
    public WebsiteListEnvelope getAll(@AuthenticationPrincipal String userId) {
        List<WebsiteResponse> list = websiteService.getAllByUser(userId).stream()
                .map(WebsiteResponse::from)
                .toList();
        return new WebsiteListEnvelope(list);
    }


    @Operation(summary = "Register a website",
            description = "The owner is taken from the token. A user id sent in the request body is ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Website registered"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "409", description = "That website URL is already registered", content = @Content),
            @ApiResponse(responseCode = "403", description = "Missing or invalid token", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WebsiteMsgEnvelope create(@AuthenticationPrincipal String userId, @Valid @RequestBody CreateWebsiteRequest request) {
        Website created = websiteService.create(userId, request.websiteName(),
                request.websiteDescription(), request.websiteUrl(), request.websiteConfiguration());
        return new WebsiteMsgEnvelope("Website created successfully", WebsiteResponse.from(created));
    }

    @Operation(summary = "Fetch one website",
            description = "Returns 404 rather than 403 when the website belongs to another user, "
                    + "so that the API never confirms the existence of someone else's data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The website"),
            @ApiResponse(responseCode = "404", description = "No such website, or it belongs to another user", content = @Content)
    })
    @GetMapping("/{id}")
    public WebsiteEnvelope getOne(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return new WebsiteEnvelope(WebsiteResponse.from(websiteService.getOwned(id, userId)));
    }

    @Operation(summary = "Check if Website exist",
            description = "Returns 404 when website doesnt exist ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Website found with given id"),
            @ApiResponse(responseCode = "404", description = "Website with given id not found", content = @Content)
    })
    @GetMapping("/exists/{id}")
    public MsgEnvelope exists(@PathVariable String id) {
        websiteService.getById(id);           // throws NotFoundException if absent — that is the whole check
        return new MsgEnvelope("Website found with id : " + id);
    }

    @Operation(summary = "Update the Website information",
            description = "Returns 404 when website doesnt exist ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Website updated successfully"),
            @ApiResponse(responseCode = "404", description = "Website with given id not found", content = @Content)
    })
    @PatchMapping("/{id}")
    public WebsiteMsgEnvelope update(@PathVariable String id,
                                     @AuthenticationPrincipal String userId,
                                     @RequestBody UpdateWebsiteRequest request) {
        Website updated = websiteService.update(id, userId, request.websiteName(),
                request.websiteDescription(), request.websiteConfiguration());
        return new WebsiteMsgEnvelope("Website updated successfully", WebsiteResponse.from(updated));
    }

    @Operation(summary = "Deletes the Website",
            description = "Returns 404 when website doesnt exist ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Website profile deleted    successfully"),
            @ApiResponse(responseCode = "404", description = "Website with given id not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public MsgEnvelope delete(@PathVariable String id, @AuthenticationPrincipal String userId) {
        websiteService.deleteWebsite(id, userId);
        return new MsgEnvelope("Website Profile deleted");
    }
}
