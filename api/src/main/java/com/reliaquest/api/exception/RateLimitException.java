package com.reliaquest.api.exception;

/**
 * Thrown when the external service returns a rate limit error (HTTP 429).
 * 
 * This is different from other external API errors because rate limiting
 * is usually temporary and the client should retry after some time.
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
