package com.reliaquest.api.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * This class handles errors for the Employee API.
 * It catches different problems and sends clear error messages.
 * It also logs errors for debugging.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles when an employee is not found.
     * Sends a not found message.
     */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFoundException(EmployeeNotFoundException ex) {
        log.info("Employee lookup failed - employee not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Employee not found")
                .message("The requested employee could not be found")
                .status(HttpStatus.NOT_FOUND.value())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles service layer errors.
     * Sends a general error message.
     */
    @ExceptionHandler(EmployeeServiceException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeServiceException(EmployeeServiceException ex) {
        log.error("Service layer error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Service error")
                .message("Unable to process your request at this time")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles errors from talking to other services.
     * Sends a message that the service is down.
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApiException(ExternalApiException ex) {
        log.error("External service communication failed: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("External service unavailable")
                .message("Our employee service is temporarily unavailable. Please try again in a few moments.")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handles validation errors from @Valid.
     * Sends details about what was wrong.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.info("Request validation failed - returning detailed field errors to client");

        // Collect all the field-specific validation errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
            log.debug("Validation error - Field: {}, Error: {}", fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Validation failed")
                .message("Please check the provided data and try again")
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles type conversion errors in URL parameters.
     *
     * This happens when someone passes a string where we expect a number,
     * or other type mismatches. Pretty common with URL parameters.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.info("Parameter type mismatch - Parameter: {}, Value: {}", ex.getName(), ex.getValue());

        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Invalid parameter")
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Catch-all handler for any exceptions we didn't specifically handle.
     *
     * This is our safety net - we don't want to ever return a raw stack trace
     * to API consumers. Better to log the details and return a generic error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred - this might need investigation: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Internal server error")
                .message("Something went wrong on our end. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
