package com.reliaquest.api.exception;

/**
 * This exception is used when we can't connect to outside services.
 * This includes problems like network issues, server errors, or bad responses from other systems. These problems aren't caused by our code, but we still need to handle them.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
