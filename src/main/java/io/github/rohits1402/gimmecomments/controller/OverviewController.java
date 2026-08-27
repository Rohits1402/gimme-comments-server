package io.github.rohits1402.gimmecomments.controller;

import io.github.rohits1402.gimmecomments.dto.OverviewResponse;
import io.github.rohits1402.gimmecomments.service.OverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Overview", description = "Totals and recent activity across the caller's websites")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/overview")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @Operation(summary = "The dashboard's front page",
            description = "Scoped to the caller throughout. Every query filters on the owner of "
                    + "the website a comment sits on, so this can never surface another user's data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Totals, fourteen days of activity, and the newest comments"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid token", content = @Content)
    })
    @GetMapping
    public OverviewResponse get(@AuthenticationPrincipal String userId) {
        return overviewService.load(userId);
    }
}
