package com.telecom.ocs.provisioning.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller implementing the /health-check endpoint.
 * Provides a simple health status response for container orchestration platforms.
 */
@RestController
public class HealthCheckController {

    /**
     * Health check endpoint returning HTTP 200 OK when service is healthy.
     * This endpoint is used by Docker, Kubernetes, and load balancers for health monitoring.
     *
     * @return ResponseEntity with OK status
     */
    @GetMapping("/health-check")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }
}
