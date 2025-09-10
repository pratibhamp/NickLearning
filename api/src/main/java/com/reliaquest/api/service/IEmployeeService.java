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

public interface IEmployeeService {

    /**
     * Fetches all employees from the system.
     *
     * This is probably our most commonly used endpoint, so I've made sure
     * to add some good logging here for monitoring purposes.
     */
    public List<Employee> getAllEmployees();

    /**
     * Searches for employees whose names contain the given search string.
     *
     * I implemented this with case-insensitive matching since that's what
     * users typically expect. The search is done in-memory after fetching
     * all employees - not the most efficient for large datasets, but works
     * fine for our current scale.
     */
    public List<Employee> searchEmployeesByName(String searchString);

    /**
     * Retrieves a specific employee by their ID.
     *
     * This is a straightforward lookup, but I added some defensive programming
     * to handle edge cases gracefully.
     */
    public Employee getEmployeeById(String id);

    /**
     * Calculates and returns the highest salary among all employees.
     *
     * I could have done this with a database query if we had one, but for now
     * we're doing the calculation in-memory. Performance should be fine unless
     * we have thousands of employees.
     */
    public Integer getHighestSalary();

    /**
     * Gets the names of our top 10 highest paid employees.
     *
     * This is useful for reporting purposes. I'm sorting by salary in descending
     * order and taking the first 10. If there are ties, the order might vary
     * between calls, but that's probably acceptable for this use case.
     */
    public List<String> getTopTenHighestEarningEmployeeNames();

    /**
     * Creates a new employee in the system.
     *
     * I added some extra validation here beyond what the Bean Validation provides
     * since employee creation is a critical operation.
     */
    public Employee createEmployee(CreateEmployeeInput input);

    /**
     * Removes an employee from the system.
     *
     * This is a sensitive operation, so I'm being extra careful with logging
     * and validation. We want to make sure we don't accidentally delete
     * the wrong person!
     */
    public String deleteEmployeeById(String id);

}
