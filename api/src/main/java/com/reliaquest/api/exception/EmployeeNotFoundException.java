package com.reliaquest.api.exception;

/**
 * Thrown when we can't find the employee someone is looking for.
 *
 * This is pretty common - employees get deleted, or users might be trying
 * to access old bookmarks. It's not really an "error" in the traditional
 * sense, more like a normal business condition.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String id) {
        super("Employee not found with id: " + id);
    }

    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
