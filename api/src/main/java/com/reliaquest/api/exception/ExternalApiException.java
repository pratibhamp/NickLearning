package com.reliaquest.api.exception;

/**
 * Thrown when we can't talk to external services properly.
 *
 * This covers network timeouts, server errors, malformed responses, and
 * other issues with external dependencies. These aren't our fault, but
 * we still need to handle them gracefully.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
