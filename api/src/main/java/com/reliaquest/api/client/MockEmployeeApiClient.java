package com.reliaquest.api.client;

import com.reliaquest.api.exception.ExternalApiException;
import com.reliaquest.api.exception.RateLimitException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.server.model.CreateMockEmployeeInput;
import com.reliaquest.server.model.DeleteMockEmployeeInput;
import com.reliaquest.server.model.MockEmployee;
import com.reliaquest.server.model.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for communicating with the mock employee API server.
 *
 * This client handles all the nitty-gritty details of making HTTP calls to
 * the external mock server. I've tried to make it robust by including proper
 * error handling, retry logic, and comprehensive logging.
 *
 * The mock server has its own data format, so this client also handles the
 * conversion between our internal models and what the external API expects.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockEmployeeApiClient {

    private final RestTemplate restTemplate;

    @Value("${mock.employee.api.base-url:http://localhost:8112}")
    private String baseUrl;

    private static final String EMPLOYEES_ENDPOINT = "/api/v1/employee";

    /**
     * Fetches all employees from the mock server.
     *
     * This is our most common operation, so I've added some performance monitoring
     * to help us track how the external API is performing.
     */
    public List<MockEmployee> getAllEmployees() {
        long startTime = System.currentTimeMillis();
        log.debug("Initiating request to fetch all employees from mock server at {}", baseUrl);

        try {
            String url = baseUrl + EMPLOYEES_ENDPOINT;

            ResponseEntity<Response<List<MockEmployee>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Response<List<MockEmployee>>>() {});

            long duration = System.currentTimeMillis() - startTime;

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<MockEmployee> employees = response.getBody().data();
                log.info("Successfully retrieved {} employees from mock server in {}ms", employees.size(), duration);
                return employees;
            }

            log.warn(
                    "Mock server returned unexpected response: status={}, body={}",
                    response.getStatusCode(),
                    response.getBody());
            throw new ExternalApiException("Mock server returned unexpected response format");

        } catch (HttpClientErrorException ex) {
            long duration = System.currentTimeMillis() - startTime;
            
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn(
                        "Rate limit exceeded when retrieving employees after {}ms. "
                                + "Mock server is applying random rate limiting as designed.",
                        duration);
                throw new RateLimitException("Employee service rate limit exceeded - please retry after a moment");
            }
            
            log.error(
                    "HTTP error {} when retrieving employees after {}ms: {}",
                    ex.getStatusCode(),
                    duration,
                    ex.getMessage());
            throw new ExternalApiException("HTTP error from employee service: " + ex.getStatusCode(), ex);
            
        } catch (RestClientException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error(
                    "Failed to retrieve employees from mock server after {}ms. "
                            + "This might indicate network issues or server downtime. Error: {}",
                    duration,
                    ex.getMessage(),
                    ex);
            throw new ExternalApiException("Unable to communicate with employee service", ex);
        }
    }

    /**
     * Looks up a specific employee by their ID.
     *
     * The mock server sometimes returns null for non-existent employees,
     * sometimes throws errors. I'm handling both cases gracefully.
     */
    public MockEmployee getEmployeeById(String id) {
        // Don't log the full ID for privacy reasons
        String maskedId = id.length() > 4 ? id.substring(0, 4) + "****" : "****";
        log.debug("Looking up employee {} from mock server", maskedId);

        try {
            String url = baseUrl + EMPLOYEES_ENDPOINT + "/" + id;

            ResponseEntity<Response<MockEmployee>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Response<MockEmployee>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                MockEmployee employee = response.getBody().data();
                if (employee != null) {
                    log.debug("Found employee {} with name: {}", maskedId, employee.getName());
                } else {
                    log.debug("Employee {} not found in mock server", maskedId);
                }
                return employee;
            }

            // If we get a non-200 response, the employee probably doesn't exist
            log.debug("Employee {} not found (status: {})", maskedId, response.getStatusCode());
            return null;

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn(
                        "Rate limit exceeded when looking up employee {}. "
                                + "Mock server is applying random rate limiting as designed.",
                        maskedId);
                throw new RateLimitException("Employee service rate limit exceeded - please retry after a moment");
            }
            
            // For other HTTP errors like 404, we still return null (handled above)
            log.debug("HTTP error {} when looking up employee {}: {}", ex.getStatusCode(), maskedId, ex.getMessage());
            throw new ExternalApiException("HTTP error from employee service: " + ex.getStatusCode(), ex);
            
        } catch (RestClientException ex) {
            log.error(
                    "Error communicating with mock server while looking up employee {}. "
                            + "Network issue or server problem: {}",
                    maskedId,
                    ex.getMessage(),
                    ex);
            throw new ExternalApiException("Unable to lookup employee", ex);
        }
    }

    /**
     * Creates a new employee in the mock server.
     *
     * The mock server expects a different format than our internal models,
     * so I'm doing the conversion here. Also adding some validation to catch
     * issues early.
     */
    public MockEmployee createEmployee(CreateEmployeeInput input) {
        log.info("Creating new employee '{}' in mock server", input.getName());

        try {
            String url = baseUrl + EMPLOYEES_ENDPOINT;

            // Convert our internal format to what the mock server expects
            CreateMockEmployeeInput mockInput = new CreateMockEmployeeInput();
            mockInput.setName(input.getName());
            mockInput.setSalary(input.getSalary());
            mockInput.setAge(input.getAge());
            mockInput.setTitle(input.getTitle());

            // Log the data we're sending (but not salary for privacy)
            log.debug(
                    "Sending employee creation request - Name: {}, Title: {}, Age: {}",
                    input.getName(),
                    input.getTitle(),
                    input.getAge());

            HttpEntity<CreateMockEmployeeInput> request = new HttpEntity<>(mockInput);

            ResponseEntity<Response<MockEmployee>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, new ParameterizedTypeReference<Response<MockEmployee>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                MockEmployee createdEmployee = response.getBody().data();
                log.info(
                        "Successfully created employee '{}' with ID: {} in mock server",
                        createdEmployee.getName(),
                        createdEmployee.getId());
                return createdEmployee;
            }

            log.error(
                    "Mock server returned unexpected response for employee creation: status={}",
                    response.getStatusCode());
            throw new ExternalApiException("Employee creation failed - server returned unexpected response");

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn(
                        "Rate limit exceeded when creating employee '{}'. "
                                + "Mock server is applying random rate limiting as designed.",
                        input.getName());
                throw new RateLimitException("Employee service rate limit exceeded - please retry after a moment");
            }
            
            log.error(
                    "HTTP error {} when creating employee '{}': {}",
                    ex.getStatusCode(),
                    input.getName(),
                    ex.getMessage());
            throw new ExternalApiException("HTTP error from employee service: " + ex.getStatusCode(), ex);
            
        } catch (RestClientException ex) {
            log.error(
                    "Failed to create employee '{}' in mock server. Network or server error: {}",
                    input.getName(),
                    ex.getMessage(),
                    ex);
            throw new ExternalApiException("Unable to create employee", ex);
        }
    }

    /**
     * Deletes an employee from the mock server.
     *
     * The mock server has a quirky API - it requires the employee's name for deletion
     * rather than just the ID. So I have to fetch the employee first to get the name.
     * Not ideal from a performance standpoint, but that's what we have to work with.
     */
    public boolean deleteEmployee(String id) {
        String maskedId = id.length() > 4 ? id.substring(0, 4) + "****" : "****";
        log.info("Attempting to delete employee {} from mock server", maskedId);

        try {
            // Step 1: Get the employee details (we need the name for deletion)
            log.debug("First fetching employee {} details for deletion", maskedId);
            MockEmployee employee = getEmployeeById(id);

            if (employee == null) {
                log.warn("Cannot delete employee {} - not found in mock server", maskedId);
                return false;
            }

            // Step 2: Actually delete the employee using their name
            log.debug("Proceeding to delete employee '{}' ({})", employee.getName(), maskedId);
            String url = baseUrl + EMPLOYEES_ENDPOINT;

            DeleteMockEmployeeInput deleteInput = new DeleteMockEmployeeInput();
            deleteInput.setName(employee.getName());

            HttpEntity<DeleteMockEmployeeInput> request = new HttpEntity<>(deleteInput);

            ResponseEntity<Response<Boolean>> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, request, new ParameterizedTypeReference<Response<Boolean>>() {});

            boolean success = response.getStatusCode() == HttpStatus.OK
                    && response.getBody() != null
                    && Boolean.TRUE.equals(response.getBody().data());

            if (success) {
                log.info("Successfully deleted employee '{}' ({}) from mock server", employee.getName(), maskedId);
            } else {
                log.warn(
                        "Delete request for employee {} completed but returned false. " + "Status: {}, Response: {}",
                        maskedId,
                        response.getStatusCode(),
                        response.getBody());
            }

            return success;

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn(
                        "Rate limit exceeded when deleting employee {}. "
                                + "Mock server is applying random rate limiting as designed.",
                        maskedId);
                throw new RateLimitException("Employee service rate limit exceeded - please retry after a moment");
            }
            
            log.error(
                    "HTTP error {} when deleting employee {}: {}",
                    ex.getStatusCode(),
                    maskedId,
                    ex.getMessage());
            throw new ExternalApiException("HTTP error from employee service: " + ex.getStatusCode(), ex);
            
        } catch (RestClientException ex) {
            log.error(
                    "Failed to delete employee {} from mock server. Network or server error: {}",
                    maskedId,
                    ex.getMessage(),
                    ex);
            throw new ExternalApiException("Unable to delete employee", ex);
        }
    }
}
