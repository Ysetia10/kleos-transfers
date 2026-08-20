package com.kleos.transfers.stats.controller;

import com.kleos.transfers.stats.dto.FitRouteResponse;
import com.kleos.transfers.stats.dto.LeagueBoardsResponse;
import com.kleos.transfers.stats.dto.ModelAccuracyResponse;
import com.kleos.transfers.stats.service.ModelAccuracyService;
import com.kleos.transfers.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "League leaderboards from ingested player-seasons")
public class StatsController {

    private final StatsService statsService;
    private final ModelAccuracyService modelAccuracyService;

    @GetMapping("/trending")
    @Operation(summary = "Top scorers and assisters for PL and La Liga in a season")
    public ResponseEntity<List<LeagueBoardsResponse>> trending(
            @RequestParam(required = false) UUID seasonId,
            @RequestParam(defaultValue = "3") int limit
    ) {
        return ResponseEntity.ok(statsService.trending(seasonId, limit));
    }

    @GetMapping("/all-time")
    @Operation(summary = "All-time scorers and assisters within PL and La Liga")
    public ResponseEntity<List<LeagueBoardsResponse>> allTime(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(statsService.allTime(limit));
    }

    @GetMapping("/fit-routes")
    @Operation(summary = "Highest-fit transfer routes (player → club) for Trending")
    public ResponseEntity<List<FitRouteResponse>> highestFitRoutes(
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(statsService.highestFitRoutes(limit));
    }

    @GetMapping("/model-accuracy")
    @Operation(summary = "Published backtest accuracy by destination league (USP)")
    public ResponseEntity<ModelAccuracyResponse> modelAccuracy() {
        return ResponseEntity.ok(modelAccuracyService.latest());
    }
}
