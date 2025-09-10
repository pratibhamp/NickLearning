package com.reliaquest.api.controller;

import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeServiceImpl;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for employee management operations.
 *
 * This controller implements the IEmployeeController interface to ensure we're
 * meeting the API contract requirements. I've tried to keep the controller
 * layer thin - most of the business logic lives in the service layer.
 *
 * All endpoints return proper HTTP status codes and include request/response
 * logging for monitoring and debugging purposes.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeInput> {

    private final EmployeeServiceImpl employeeServiceImpl;

    /**
     * Retrieves all employees in the system.
     *
     * This is likely to be our most hit endpoint, so I've made sure to include
     * performance logging to help us monitor response times.
     */
    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        long startTime = System.currentTimeMillis();
        log.info("GET /api/v1/employee - Request received to fetch all employees");

        try {
            List<Employee> employees = employeeServiceImpl.getAllEmployees();
            long duration = System.currentTimeMillis() - startTime;

            log.info("GET /api/v1/employee - Successfully returned {} employees in {}ms", employees.size(), duration);

            return ResponseEntity.ok(employees);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("GET /api/v1/employee - Request failed after {}ms. Error: {}", duration, ex.getMessage(), ex);
            throw ex; // Let the global exception handler deal with it
        }
    }

    /**
     * Searches for employees by name.
     *
     * I decided to log the search terms for analytics purposes, but I'm being
     * careful not to log any sensitive information.
     */
    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(String searchString) {
        log.info("GET /api/v1/employee/search/{} - Employee search requested", searchString);

        // Basic sanitization for logging - remove any potential injection attempts
        String sanitizedSearchString = searchString.replaceAll("[\r\n\t]", "");

        try {
            List<Employee> employees = employeeServiceImpl.searchEmployeesByName(searchString);

            log.info(
                    "GET /api/v1/employee/search/{} - Search completed, found {} matching employees",
                    sanitizedSearchString,
                    employees.size());

            return ResponseEntity.ok(employees);

        } catch (Exception ex) {
            log.error(
                    "GET /api/v1/employee/search/{} - Search failed. Error: {}",
                    sanitizedSearchString,
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    /**
     * Fetches a specific employee by their ID.
     *
     * This is a simple lookup operation, but employee IDs are sensitive
     * information so I'm being careful about what gets logged.
     */
    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        // Mask the ID in logs for privacy (show first 4 chars only)
        String maskedId = id.length() > 4 ? id.substring(0, 4) + "****" : "****";
        log.info("GET /api/v1/employee/{} - Looking up employee by ID", maskedId);

        try {
            Employee employee = employeeServiceImpl.getEmployeeById(id);

            log.info("GET /api/v1/employee/{} - Successfully found employee: {}", maskedId, employee.getName());

            return ResponseEntity.ok(employee);

        } catch (Exception ex) {
            log.error("GET /api/v1/employee/{} - Lookup failed. Error: {}", maskedId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Returns the highest salary in the system.
     *
     * This is useful for HR analytics and reporting. Pretty straightforward
     * endpoint, but the calculation might be expensive with lots of employees.
     */
    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.info("GET /api/v1/employee/highestSalary - Calculating highest salary");

        try {
            Integer highestSalary = employeeServiceImpl.getHighestSalary();

            log.info("GET /api/v1/employee/highestSalary - Highest salary calculated: ${:,}", highestSalary);

            return ResponseEntity.ok(highestSalary);

        } catch (Exception ex) {
            log.error("GET /api/v1/employee/highestSalary - Calculation failed. Error: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Gets the names of our top 10 highest paid employees.
     *
     * This endpoint is probably used for executive reporting. I'm not logging
     * the actual names for privacy reasons, just the count.
     */
    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.info("GET /api/v1/employee/topTenHighestEarningEmployeeNames - Fetching top earners");

        try {
            List<String> topEarners = employeeServiceImpl.getTopTenHighestEarningEmployeeNames();

            log.info(
                    "GET /api/v1/employee/topTenHighestEarningEmployeeNames - Retrieved {} top earner names",
                    topEarners.size());

            return ResponseEntity.ok(topEarners);

        } catch (Exception ex) {
            log.error(
                    "GET /api/v1/employee/topTenHighestEarningEmployeeNames - Request failed. Error: {}",
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    /**
     * Creates a new employee in the system.
     *
     * This is a critical operation that changes system state, so I'm logging
     * key details for audit purposes. The @Valid annotation ensures our
     * validation rules are enforced.
     */
    @Override
    public ResponseEntity<Employee> createEmployee(@Valid CreateEmployeeInput employeeInput) {
        log.info(
                "POST /api/v1/employee - Creating new employee: {} ({})",
                employeeInput.getName(),
                employeeInput.getEmail());

        // Log some key details for auditing (but not salary for privacy)
        log.debug(
                "POST /api/v1/employee - New employee details: Name={}, Title={}, Age={}",
                employeeInput.getName(),
                employeeInput.getTitle(),
                employeeInput.getAge());

        try {
            Employee createdEmployee = employeeServiceImpl.createEmployee(employeeInput);

            log.info(
                    "POST /api/v1/employee - Successfully created employee with ID: {} for {}",
                    createdEmployee.getId(),
                    createdEmployee.getName());

            // Return 201 Created for successful resource creation
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);

        } catch (Exception ex) {
            log.error(
                    "POST /api/v1/employee - Failed to create employee '{}'. Error: {}",
                    employeeInput.getName(),
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    /**
     * Deletes an employee from the system.
     *
     * This is a destructive operation, so I'm being extra careful with logging.
     * We want a clear audit trail of who gets deleted and when.
     */
    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        // Mask the ID for privacy in logs
        String maskedId = id.length() > 4 ? id.substring(0, 4) + "****" : "****";
        log.info("DELETE /api/v1/employee/{} - Delete request received", maskedId);

        try {
            String result = employeeServiceImpl.deleteEmployeeById(id);

            log.info("DELETE /api/v1/employee/{} - Employee successfully deleted", maskedId);

            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            log.error("DELETE /api/v1/employee/{} - Delete operation failed. Error: {}", maskedId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
