package com.reliaquest.api.client;

import com.reliaquest.api.exception.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test to demonstrate that the API properly handles HTTP 429 responses
 * from the RandomRequestLimitInterceptor in the server module.
 */
public class RateLimitHandlingTest {

    private MockEmployeeApiClient client;
    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() {
        mockRestTemplate = mock(RestTemplate.class);
        client = new MockEmployeeApiClient(mockRestTemplate);
        // Set the base URL using reflection since it's a @Value field
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8112");
    }

    @Test
    void testGetAllEmployees_HandlesRateLimitException() {
        // Arrange: Mock RestTemplate to throw HttpClientErrorException with 429 status
        HttpClientErrorException rateLimitException = 
            new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        
        when(mockRestTemplate.exchange(
            any(String.class),
            eq(org.springframework.http.HttpMethod.GET),
            any(),
            any(org.springframework.core.ParameterizedTypeReference.class)
        )).thenThrow(rateLimitException);

        // Act & Assert: Verify that RateLimitException is thrown
        RateLimitException thrown = assertThrows(RateLimitException.class, () -> {
            client.getAllEmployees();
        });

        // Verify the exception message
        assertTrue(thrown.getMessage().contains("rate limit exceeded"));
        assertTrue(thrown.getMessage().toLowerCase().contains("retry"));
    }

    @Test
    void testGetEmployeeById_HandlesRateLimitException() {
        // Arrange: Mock RestTemplate to throw HttpClientErrorException with 429 status
        HttpClientErrorException rateLimitException = 
            new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        
        when(mockRestTemplate.exchange(
            any(String.class),
            eq(org.springframework.http.HttpMethod.GET),
            any(),
            any(org.springframework.core.ParameterizedTypeReference.class)
        )).thenThrow(rateLimitException);

        // Act & Assert: Verify that RateLimitException is thrown
        RateLimitException thrown = assertThrows(RateLimitException.class, () -> {
            client.getEmployeeById("test-id");
        });

        // Verify the exception message
        assertTrue(thrown.getMessage().contains("rate limit exceeded"));
    }
}
