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
 * This class sends HTTP requests to the mock employee API server.
 * It changes our data to the format the server needs.
 * It also handles errors if something goes wrong.
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
     * Get all employees from the mock server.
     * Shows how long the request took.
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
     * Get an employee by their ID.
     * Returns null if not found.
     */
    public MockEmployee getEmployeeById(String id) {
        // Hide most of the ID for privacy
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

            // If not found, return null
            log.debug("Employee {} not found (status: {})", maskedId, response.getStatusCode());
            return null;

        } catch (RestClientException ex) {
            log.error("Error talking to mock server for employee {}. Error: {}", maskedId, ex.getMessage(), ex);
            throw new ExternalApiException("Could not get employee", ex);
        }
    }

    /**
     * Add a new employee to the mock server.
     * Changes our data to the server's format.
     */
    public MockEmployee createEmployee(CreateEmployeeInput input) {
        log.info("Adding new employee '{}' to mock server", input.getName());

        try {
            String url = baseUrl + EMPLOYEES_ENDPOINT;

            // Change our format to the server's format
            CreateMockEmployeeInput mockInput = new CreateMockEmployeeInput();
            mockInput.setName(input.getName());
            mockInput.setSalary(input.getSalary());
            mockInput.setAge(input.getAge());
            mockInput.setTitle(input.getTitle());

            // Log what we are sending (not salary)
            log.debug("Sending employee create request - Name: {}, Title: {}, Age: {}", input.getName(), input.getTitle(), input.getAge());

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
     * Delete an employee from the mock server.
     * The server needs the employee's name, so we get the employee first.
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
