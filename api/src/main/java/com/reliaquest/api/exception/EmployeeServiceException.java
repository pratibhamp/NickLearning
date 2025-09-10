package com.reliaquest.api.exception;

/**
 * This exception is used when something goes wrong in our business logic.
 * This usually means there is a problem with our code or business rules,
 * not with outside systems. Examples include validation problems,
 * data mistakes, or breaking business rules.
 */
public class EmployeeServiceException extends RuntimeException {

    public EmployeeServiceException(String message) {
        super(message);
    }

    public EmployeeServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
