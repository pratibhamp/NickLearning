package com.reliaquest.server.controller;

import com.reliaquest.server.config.RateLimitingProperties;
import com.reliaquest.server.model.Response;
import com.reliaquest.server.service.RateLimitingService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing and monitoring rate limiting.
 * Only enabled when management endpoints are enabled in configuration.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/rate-limit")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.management.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitController {

    private final RateLimitingService rateLimitingService;
    private final RateLimitingProperties rateLimitingProperties;

    /**
     * Get current rate limiting configuration.
     */
    @GetMapping("/config")
    public Response<RateLimitingProperties> getConfiguration() {
        log.debug("Retrieving rate limiting configuration");
        return Response.handledWith(rateLimitingProperties);
    }

    /**
     * Get rate limiting statistics.
     */
    @GetMapping("/stats")
    public Response<Map<String, Object>> getStatistics() {
        log.debug("Retrieving rate limiting statistics");

        Map<String, Object> stats = Map.of(
                "enabled", rateLimitingProperties.isEnabled(),
                "activeBuckets", rateLimitingService.getBucketCount(),
                "globalConfig", rateLimitingProperties.getGlobal(),
                "endpointConfigs", rateLimitingProperties.getEndpoints());

        return Response.handledWith(stats);
    }

    /**
     * Clear all rate limiting buckets (reset all rate limits).
     */
    @PostMapping("/reset")
    public Response<String> resetRateLimits() {
        log.info("Resetting all rate limiting buckets");
        rateLimitingService.clearAllBuckets();
        return Response.handledWith("All rate limiting buckets have been cleared");
    }

    /**
     * Check if rate limiting is enabled.
     */
    @GetMapping("/status")
    public Response<Map<String, Object>> getStatus() {
        Map<String, Object> status = Map.of(
                "enabled", rateLimitingProperties.isEnabled(),
                "globalEnabled", rateLimitingProperties.getGlobal().isEnabled(),
                "endpointCount", rateLimitingProperties.getEndpoints().size());

        return Response.handledWith(status);
    }
}
