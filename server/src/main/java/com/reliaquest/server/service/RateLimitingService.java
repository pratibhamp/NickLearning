package com.reliaquest.server.service;

import com.reliaquest.server.config.RateLimitingProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

/**
 * Service for managing rate limiting using Bucket4j.
 * Supports both global and endpoint-specific rate limiting with configurable parameters.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RateLimitingProperties rateLimitingProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Checks if a request is allowed based on the client identifier and request path.
     *
     * @param clientId the client identifier (e.g., IP address)
     * @param requestPath the request path
     * @return true if the request is allowed, false if rate limited
     */
    public boolean isRequestAllowed(String clientId, String requestPath) {
        if (!rateLimitingProperties.isEnabled()) {
            return true;
        }

        // Find the most specific rate limit configuration
        RateLimitingProperties.RateLimitConfig config = findRateLimitConfig(requestPath);
        if (!config.isEnabled()) {
            return true;
        }

        // Create a unique bucket key combining client ID and config
        String bucketKey = createBucketKey(clientId, requestPath, config);
        Bucket bucket = buckets.computeIfAbsent(bucketKey, key -> createBucket(config));

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for client: {} on path: {}", clientId, requestPath);
        } else {
            log.debug(
                    "Request allowed for client: {} on path: {}, remaining tokens: {}",
                    clientId,
                    requestPath,
                    bucket.getAvailableTokens());
        }

        return allowed;
    }

    /**
     * Gets the error message for rate limit exceeded based on the request path.
     *
     * @param requestPath the request path
     * @return the configured error message
     */
    public String getRateLimitErrorMessage(String requestPath) {
        RateLimitingProperties.RateLimitConfig config = findRateLimitConfig(requestPath);
        return config.getErrorMessage();
    }

    /**
     * Finds the most specific rate limit configuration for the given request path.
     */
    private RateLimitingProperties.RateLimitConfig findRateLimitConfig(String requestPath) {
        // Check endpoint-specific configurations first
        for (Map.Entry<String, RateLimitingProperties.RateLimitConfig> entry :
                rateLimitingProperties.getEndpoints().entrySet()) {
            if (pathMatcher.match(entry.getKey(), requestPath)) {
                log.debug(
                        "Using endpoint-specific rate limit for path: {} with pattern: {}",
                        requestPath,
                        entry.getKey());
                return entry.getValue();
            }
        }

        // Fall back to global configuration
        log.debug("Using global rate limit for path: {}", requestPath);
        return rateLimitingProperties.getGlobal();
    }

    /**
     * Creates a unique bucket key for the given parameters.
     */
    private String createBucketKey(String clientId, String requestPath, RateLimitingProperties.RateLimitConfig config) {
        return String.format(
                "%s:%s:%d:%d:%s",
                clientId, requestPath, config.getCapacity(), config.getRefillTokens(), config.getRefillPeriod());
    }

    /**
     * Creates a new bucket with the specified configuration.
     */
    private Bucket createBucket(RateLimitingProperties.RateLimitConfig config) {
        Bandwidth bandwidth = Bandwidth.classic(
                config.getCapacity(), Refill.intervally(config.getRefillTokens(), config.getRefillPeriod()));

        Bucket bucket = Bucket.builder().addLimit(bandwidth).build();

        log.debug(
                "Created new rate limit bucket with capacity: {}, refill: {} tokens every {}",
                config.getCapacity(),
                config.getRefillTokens(),
                config.getRefillPeriod());

        return bucket;
    }

    /**
     * Clears all buckets (useful for testing or manual reset).
     */
    public void clearAllBuckets() {
        buckets.clear();
        log.info("All rate limit buckets cleared");
    }

    /**
     * Gets the current number of buckets (useful for monitoring).
     */
    public int getBucketCount() {
        return buckets.size();
    }
}
