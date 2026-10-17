package com.agentic.platform.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getRootStatus() {
        return ResponseEntity.ok(Map.of(
                "service", "Satset Autonomous Agentic Orchestrator Engine",
                "version", "1.0.0-SNAPSHOT",
                "status", "UP",
                "timestamp", Instant.now(),
                "endpoints", Map.of(
                        "workflows", "/api/workflow",
                        "workflowStream", "/api/workflow/stream",
                        "h2Console", "/h2-console",
                        "dashboardUI", "http://localhost:3000"
                ),
                "features", Map.of(
                        "multiProjectIsolation", true,
                        "triadCouncilConsensus", true,
                        "virtualThreads", true
                )
        ));
    }
}
