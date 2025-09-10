package com.reliaquest.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for rate limiting functionality
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowRequestsWithinRateLimit() throws Exception {
        // Make requests within the limit (first 10 should succeed)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/employee")
                    .header("X-Forwarded-For", "127.0.0.1"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldBlockRequestsExceedingRateLimit() throws Exception {
        String clientIp = "192.168.1.100";
        
        // Exhaust the rate limit (10 requests for /api/v1/employee/**)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/employee")
                    .header("X-Forwarded-For", clientIp))
                    .andExpect(status().isOk());
        }
        
        // 11th request should be rate limited
        mockMvc.perform(get("/api/v1/employee")
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void shouldApplyDifferentLimitsForDifferentEndpoints() throws Exception {
        String clientIp = "192.168.1.200";
        
        // Test specific employee endpoint (should have 10 request limit)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/employee/test-id")
                    .header("X-Forwarded-For", clientIp))
                    .andExpect(status().isNotFound()); // Employee not found, but not rate limited
        }
        
        // 11th request should be rate limited
        mockMvc.perform(get("/api/v1/employee/test-id")
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldHandleManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/rate-limit/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void shouldResetRateLimits() throws Exception {
        String clientIp = "192.168.1.300";
        
        // Exhaust rate limit
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/employee")
                    .header("X-Forwarded-For", clientIp));
        }
        
        // Reset rate limits
        mockMvc.perform(post("/api/v1/admin/rate-limit/reset"))
                .andExpect(status().isOk());
        
        // Should be able to make requests again
        mockMvc.perform(get("/api/v1/employee")
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk());
    }
}
