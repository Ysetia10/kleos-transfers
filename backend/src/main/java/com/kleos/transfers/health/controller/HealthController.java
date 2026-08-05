package com.kleos.transfers.health.controller;

import com.kleos.transfers.health.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final String applicationName;
    private final String applicationVersion;

    public HealthController(
            @Value("${kleos.application.name}") String applicationName,
            @Value("${kleos.application.version}") String applicationVersion
    ) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", applicationName, applicationVersion);
    }
}
