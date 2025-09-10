package com.reliaquest.api.exception;

/**
 * Thrown when something goes wrong in our business logic layer.
 *
 * This usually indicates a problem with our code or business rules,
 * rather than external dependencies. Things like validation failures,
 * data inconsistencies, or business rule violations.
 */
public class EmployeeServiceException extends RuntimeException {

    public EmployeeServiceException(String message) {
        super(message);
    }

    public EmployeeServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
