package com.reliaquest.api.service;

import com.reliaquest.api.client.MockEmployeeApiClient;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.ExternalApiException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.server.model.MockEmployee;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Central service for managing employee operations.
 *
 * This service acts as the main business logic layer, coordinating between
 * the web layer and external services. It handles data transformation,
 * validation, and error management.
 *
 * Note: We're using the mock employee API as our data source, but this
 * service is designed to be flexible enough to switch to a database
 * or other data sources in the future.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements IEmployeeService {

    private final MockEmployeeApiClient mockEmployeeApiClient;

    /**
     * Fetches all employees from the system.
     *
     * This is probably our most commonly used endpoint, so I've made sure
     * to add some good logging here for monitoring purposes.
     */
    public List<Employee> getAllEmployees() {
        log.info("Starting to fetch all employees from the system");

        try {
            List<MockEmployee> mockEmployees = mockEmployeeApiClient.getAllEmployees();
            log.debug("Successfully retrieved {} employees from external API", mockEmployees.size());

            // Convert the external format to our internal Employee format
            List<Employee> employees =
                    mockEmployees.stream().map(this::convertToEmployee).collect(Collectors.toList());

            log.info("Successfully processed and returned {} employees", employees.size());
            return employees;

        } catch (ExternalApiException ex) {
            log.error("External API communication failed while retrieving employees: {}", ex.getMessage(), ex);
            throw ex; // Re-throw to be handled by GlobalExceptionHandler
        } catch (Exception ex) {
            log.error("Failed to retrieve employees from external service. This could impact user experience.", ex);
            throw new EmployeeServiceException("Unable to fetch employees at this time", ex);
        }
    }

    /**
     * Searches for employees whose names contain the given search string.
     *
     * I implemented this with case-insensitive matching since that's what
     * users typically expect. The search is done in-memory after fetching
     * all employees - not the most efficient for large datasets, but works
     * fine for our current scale.
     */
    public List<Employee> searchEmployeesByName(String searchString) {
        log.info("Searching for employees with name containing: '{}'", searchString);

        // Basic input validation - empty searches don't make sense
        if (!StringUtils.hasText(searchString)) {
            log.warn("Attempted to search with empty or null search string");
            throw new EmployeeServiceException("Search string cannot be empty");
        }

        try {
            List<MockEmployee> allEmployees = mockEmployeeApiClient.getAllEmployees();
            log.debug("Retrieved {} total employees to search through", allEmployees.size());

            String lowerSearchString = searchString.toLowerCase().trim();

            List<Employee> matchingEmployees = allEmployees.stream()
                    .filter(employee -> {
                        boolean matches = employee.getName().toLowerCase().contains(lowerSearchString);
                        if (matches) {
                            log.trace("Found match: {}", employee.getName());
                        }
                        return matches;
                    })
                    .map(this::convertToEmployee)
                    .collect(Collectors.toList());

            log.info("Search for '{}' returned {} matching employees", searchString, matchingEmployees.size());
            return matchingEmployees;

        } catch (Exception ex) {
            log.error("Search operation failed for query: '{}'. Error: {}", searchString, ex.getMessage(), ex);
            throw new EmployeeServiceException("Search operation failed", ex);
        }
    }

    /**
     * Retrieves a specific employee by their ID.
     *
     * This is a straightforward lookup, but I added some defensive programming
     * to handle edge cases gracefully.
     */
    public Employee getEmployeeById(String id) {
        log.info("Looking up employee with ID: {}", id);

        if (!StringUtils.hasText(id)) {
            log.warn("Attempted to fetch employee with empty or null ID");
            throw new EmployeeServiceException("Employee ID cannot be empty");
        }

        try {
            MockEmployee mockEmployee = mockEmployeeApiClient.getEmployeeById(id);

            if (mockEmployee == null) {
                log.info("No employee found with ID: {}", id);
                throw new EmployeeNotFoundException(id);
            }

            log.debug("Successfully found employee: {} ({})", mockEmployee.getName(), id);
            return convertToEmployee(mockEmployee);

        } catch (EmployeeNotFoundException ex) {
            // Re-throw as is - this is expected behavior
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while fetching employee with ID: {}. Error: {}", id, ex.getMessage(), ex);
            throw new EmployeeServiceException("Failed to retrieve employee", ex);
        }
    }

    /**
     * Calculates and returns the highest salary among all employees.
     *
     * I could have done this with a database query if we had one, but for now
     * we're doing the calculation in-memory. Performance should be fine unless
     * we have thousands of employees.
     */
    public Integer getHighestSalary() {
        log.info("Calculating highest salary across all employees");

        try {
            List<MockEmployee> allEmployees = mockEmployeeApiClient.getAllEmployees();
            log.debug("Analyzing salaries for {} employees", allEmployees.size());

            Integer highestSalary = allEmployees.stream()
                    .map(MockEmployee::getSalary)
                    .max(Integer::compareTo)
                    .orElse(0);

            log.info("Highest salary found: ${:,}", highestSalary);
            return highestSalary;

        } catch (Exception ex) {
            log.error("Failed to calculate highest salary. Error: {}", ex.getMessage(), ex);
            throw new EmployeeServiceException("Unable to calculate highest salary", ex);
        }
    }

    /**
     * Gets the names of our top 10 highest paid employees.
     *
     * This is useful for reporting purposes. I'm sorting by salary in descending
     * order and taking the first 10. If there are ties, the order might vary
     * between calls, but that's probably acceptable for this use case.
     */
    public List<String> getTopTenHighestEarningEmployeeNames() {
        log.info("Fetching names of top 10 highest earning employees");

        try {
            List<MockEmployee> allEmployees = mockEmployeeApiClient.getAllEmployees();
            log.debug("Ranking {} employees by salary", allEmployees.size());

            List<String> topEarnerNames = allEmployees.stream()
                    .sorted(Comparator.comparing(MockEmployee::getSalary).reversed())
                    .limit(10)
                    .map(employee -> {
                        log.trace("Top earner: {} (${:,})", employee.getName(), employee.getSalary());
                        return employee.getName();
                    })
                    .collect(Collectors.toList());

            log.info("Successfully identified {} top earning employees", topEarnerNames.size());
            return topEarnerNames;

        } catch (Exception ex) {
            log.error("Failed to get top earning employees. Error: {}", ex.getMessage(), ex);
            throw new EmployeeServiceException("Unable to retrieve top earning employees", ex);
        }
    }

    /**
     * Creates a new employee in the system.
     *
     * I added some extra validation here beyond what the Bean Validation provides
     * since employee creation is a critical operation.
     */
    public Employee createEmployee(CreateEmployeeInput input) {
        if (input == null) {
            log.warn("Attempted to create employee with null input");
            throw new EmployeeServiceException("Employee input cannot be null");
        }

        log.info("Creating new employee: {} ({})", input.getName(), input.getEmail());

        // Log some key details for audit purposes
        log.debug(
                "New employee details - Name: {}, Title: {}, Salary: ${:,}, Age: {}",
                input.getName(),
                input.getTitle(),
                input.getSalary(),
                input.getAge());

        try {
            MockEmployee createdEmployee = mockEmployeeApiClient.createEmployee(input);
            log.info(
                    "Successfully created employee with ID: {} for {}",
                    createdEmployee.getId(),
                    createdEmployee.getName());

            return convertToEmployee(createdEmployee);

        } catch (Exception ex) {
            log.error("Failed to create employee '{}'. Error: {}", input.getName(), ex.getMessage(), ex);
            throw new EmployeeServiceException("Failed to create employee", ex);
        }
    }

    /**
     * Removes an employee from the system.
     *
     * This is a sensitive operation, so I'm being extra careful with logging
     * and validation. We want to make sure we don't accidentally delete
     * the wrong person!
     */
    public String deleteEmployeeById(String id) {
        if (!StringUtils.hasText(id)) {
            log.warn("Attempted to delete employee with empty or null ID");
            throw new EmployeeServiceException("Employee ID cannot be empty");
        }

        log.info("Attempting to delete employee with ID: {}", id);

        try {
            // First, let's make sure the employee exists
            MockEmployee existingEmployee = mockEmployeeApiClient.getEmployeeById(id);
            if (existingEmployee == null) {
                log.warn("Cannot delete employee with ID {} - employee not found", id);
                throw new EmployeeNotFoundException(id);
            }

            // Log who we're about to delete for audit trail
            log.info(
                    "Confirmed employee exists: {} ({}). Proceeding with deletion.",
                    existingEmployee.getName(),
                    existingEmployee.getEmail());

            boolean deleted = mockEmployeeApiClient.deleteEmployee(id);

            if (deleted) {
                log.info("Successfully deleted employee: {} (ID: {})", existingEmployee.getName(), id);
                return "Employee deleted successfully";
            } else {
                log.error("Delete operation returned false for employee ID: {}. This is unexpected.", id);
                throw new EmployeeServiceException("Delete operation failed - please try again");
            }

        } catch (EmployeeNotFoundException ex) {
            // Re-throw as is - this is expected behavior
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Unexpected error during delete operation for employee ID: {}. Error: {}", id, ex.getMessage(), ex);
            throw new EmployeeServiceException("Failed to delete employee", ex);
        }
    }

    /**
     * Converts a MockEmployee from the external API to our internal Employee format.
     *
     * This is a simple mapping operation, but I've isolated it into its own method
     * to make testing easier and to keep the conversion logic in one place.
     */
    private Employee convertToEmployee(MockEmployee mockEmployee) {
        if (mockEmployee == null) {
            log.warn("Attempted to convert null MockEmployee to Employee");
            return null;
        }

        return Employee.builder()
                .id(mockEmployee.getId() != null ? mockEmployee.getId().toString() : null)
                .name(mockEmployee.getName())
                .salary(mockEmployee.getSalary())
                .age(mockEmployee.getAge())
                .title(mockEmployee.getTitle())
                .email(mockEmployee.getEmail())
                .build();
    }
}
