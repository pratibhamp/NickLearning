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
 * This class does the main work for employee actions.
 * It talks to the web layer and other services.
 * It checks data and handles errors.
 * It can be changed to use a database later.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements IEmployeeService {

    private final MockEmployeeApiClient mockEmployeeApiClient;

    /**
     * Get all employees.
     * Converts data to our format.
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
     * Search employees by name (case-insensitive).
     * Checks for empty search.
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
     * Get one employee by ID.
     * Checks for empty ID and handles not found.
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
     * Get the highest salary.
     * Calculates in memory.
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
     * Get names of top 10 highest paid employees.
     * Sorts by salary and picks first 10.
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
     * Add a new employee.
     * Checks input and logs details.
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
     * Delete an employee by ID.
     * Checks for empty ID and makes sure employee exists.
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
     * Change a MockEmployee to our Employee format.
     * Returns null if input is null.
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
