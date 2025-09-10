package com.reliaquest.server.service;

import com.reliaquest.server.config.RateLimitingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RateLimitingService
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;
    private RateLimitingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        properties.setEnabled(true);
        
        // Configure global settings
        RateLimitingProperties.RateLimitConfig globalConfig = new RateLimitingProperties.RateLimitConfig();
        globalConfig.setCapacity(20);
        globalConfig.setRefillTokens(20);
        globalConfig.setRefillPeriod(Duration.ofMinutes(1));
        globalConfig.setEnabled(true);
        globalConfig.setErrorMessage("Global rate limit exceeded");
        properties.setGlobal(globalConfig);
        
        // Configure endpoint-specific settings
        RateLimitingProperties.RateLimitConfig employeeConfig = new RateLimitingProperties.RateLimitConfig();
        employeeConfig.setCapacity(10);
        employeeConfig.setRefillTokens(10);
        employeeConfig.setRefillPeriod(Duration.ofMinutes(1));
        employeeConfig.setEnabled(true);
        employeeConfig.setErrorMessage("Employee API rate limit exceeded");
        
        properties.setEndpoints(Map.of("/api/v1/employee/**", employeeConfig));
        
        rateLimitingService = new RateLimitingService(properties);
    }

    @Test
    void shouldAllowRequestsWithinLimit() {
        String clientId = "127.0.0.1";
        String path = "/api/v1/employee";
        
        // Should allow first 10 requests (endpoint-specific limit)
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimitingService.isRequestAllowed(clientId, path))
                .as("Request %d should be allowed", i + 1)
                .isTrue();
        }
    }

    @Test
    void shouldBlockRequestsExceedingLimit() {
        String clientId = "127.0.0.1";
        String path = "/api/v1/employee/123";
        
        // Exhaust the limit (10 requests)
        for (int i = 0; i < 10; i++) {
            rateLimitingService.isRequestAllowed(clientId, path);
        }
        
        // 11th request should be blocked
        assertThat(rateLimitingService.isRequestAllowed(clientId, path))
            .as("Request exceeding limit should be blocked")
            .isFalse();
    }

    @Test
    void shouldUseDifferentBucketsForDifferentClients() {
        String client1 = "127.0.0.1";
        String client2 = "192.168.1.1";
        String path = "/api/v1/employee";
        
        // Exhaust limit for client1
        for (int i = 0; i < 10; i++) {
            rateLimitingService.isRequestAllowed(client1, path);
        }
        
        // Client1 should be blocked
        assertThat(rateLimitingService.isRequestAllowed(client1, path)).isFalse();
        
        // Client2 should still be allowed
        assertThat(rateLimitingService.isRequestAllowed(client2, path)).isTrue();
    }

    @Test
    void shouldReturnCorrectErrorMessage() {
        String path = "/api/v1/employee/123";
        String errorMessage = rateLimitingService.getRateLimitErrorMessage(path);
        
        assertThat(errorMessage).isEqualTo("Employee API rate limit exceeded");
    }

    @Test
    void shouldUseGlobalConfigWhenNoEndpointMatch() {
        String clientId = "127.0.0.1";
        String path = "/api/v1/other-endpoint";
        
        // Should use global config (20 requests)
        for (int i = 0; i < 20; i++) {
            assertThat(rateLimitingService.isRequestAllowed(clientId, path))
                .as("Request %d should use global limit", i + 1)
                .isTrue();
        }
        
        // 21st request should be blocked
        assertThat(rateLimitingService.isRequestAllowed(clientId, path)).isFalse();
    }

    @Test
    void shouldAllowAllRequestsWhenDisabled() {
        properties.setEnabled(false);
        rateLimitingService = new RateLimitingService(properties);
        
        String clientId = "127.0.0.1";
        String path = "/api/v1/employee";
        
        // Should allow unlimited requests when disabled
        for (int i = 0; i < 100; i++) {
            assertThat(rateLimitingService.isRequestAllowed(clientId, path))
                .as("All requests should be allowed when rate limiting is disabled")
                .isTrue();
        }
    }

    @Test
    void shouldClearAllBuckets() {
        String clientId = "127.0.0.1";
        String path = "/api/v1/employee";
        
        // Create some buckets
        rateLimitingService.isRequestAllowed(clientId, path);
        assertThat(rateLimitingService.getBucketCount()).isGreaterThan(0);
        
        // Clear all buckets
        rateLimitingService.clearAllBuckets();
        assertThat(rateLimitingService.getBucketCount()).isEqualTo(0);
    }
}
