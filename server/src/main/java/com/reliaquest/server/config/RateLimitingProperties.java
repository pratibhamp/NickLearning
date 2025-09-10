package com.reliaquest.server.config;

import java.time.Duration;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for rate limiting.
 * Supports both global and endpoint-specific rate limiting configurations.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitingProperties {

    /**
     * Whether rate limiting is enabled globally
     */
    private boolean enabled = true;

    /**
     * Global rate limiting configuration
     */
    private RateLimitConfig global = new RateLimitConfig();

    /**
     * Endpoint-specific rate limiting configurations
     * Key: endpoint pattern (e.g., "/api/v1/employee/**")
     * Value: rate limit configuration for that endpoint
     */
    private Map<String, RateLimitConfig> endpoints = Map.of();

    @Data
    public static class RateLimitConfig {
        /**
         * Maximum number of requests allowed
         */
        private int capacity = 10;

        /**
         * Number of tokens to refill
         */
        private int refillTokens = 10;

        /**
         * Time period for refill (e.g., PT1M for 1 minute)
         */
        private Duration refillPeriod = Duration.ofMinutes(1);

        /**
         * Whether this rate limit configuration is enabled
         */
        private boolean enabled = true;

        /**
         * Custom error message for rate limit exceeded
         */
        private String errorMessage = "Rate limit exceeded. Please try again later.";
    }
}
