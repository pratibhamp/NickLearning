package com.reliaquest.api.exception;

/**
 * This exception is used when an employee can't be found.
 * This happens often if employees are deleted or someone tries to use an old link. It's not a serious error, just a normal situation in business.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String id) {
        super("Employee not found with id: " + id);
    }

    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
