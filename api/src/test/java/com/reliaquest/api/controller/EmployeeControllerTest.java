package com.reliaquest.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeServiceImpl;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({EmployeeController.class, com.reliaquest.api.exception.GlobalExceptionHandler.class})
@DisplayName("Employee Controller Tests")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeServiceImpl employeeServiceImpl;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee employee1;
    private Employee employee2;
    private CreateEmployeeInput createEmployeeInput;

    @BeforeEach
    void setUp() {
        employee1 = Employee.builder()
                .id(UUID.randomUUID().toString())
                .name("John Doe")
                .salary(75000)
                .age(30)
                .title("Software Engineer")
                .email("john.doe@example.com")
                .build();

        employee2 = Employee.builder()
                .id(UUID.randomUUID().toString())
                .name("Jane Smith")
                .salary(85000)
                .age(28)
                .title("Senior Software Engineer")
                .email("jane.smith@example.com")
                .build();

        createEmployeeInput = CreateEmployeeInput.builder()
                .name("New Employee")
                .salary(70000)
                .age(25)
                .title("Junior Developer")
                .email("new.employee@example.com")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/employee should return all employees")
    void getAllEmployees_ShouldReturnAllEmployees() throws Exception {
        // Given
        List<Employee> employees = Arrays.asList(employee1, employee2);
        when(employeeServiceImpl.getAllEmployees()).thenReturn(employees);

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employee_name").value("John Doe"))
                .andExpect(jsonPath("$[1].employee_name").value("Jane Smith"));
    }

    @Test
    @DisplayName("GET /api/v1/employee/search/{searchString} should return matching employees")
    void getEmployeesByNameSearch_ShouldReturnMatchingEmployees() throws Exception {
        // Given
        List<Employee> matchingEmployees = Arrays.asList(employee1);
        when(employeeServiceImpl.searchEmployeesByName("John")).thenReturn(matchingEmployees);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/search/John"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employee_name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/employee/{id} should return employee by ID")
    void getEmployeeById_ShouldReturnEmployee() throws Exception {
        // Given
        String employeeId = employee1.getId();
        when(employeeServiceImpl.getEmployeeById(employeeId)).thenReturn(employee1);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.employee_name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/employee/{id} should return 404 when employee not found")
    void getEmployeeById_WhenEmployeeNotFound_ShouldReturn404() throws Exception {
        // Given
        String nonExistentId = "non-existent-id";
        when(employeeServiceImpl.getEmployeeById(nonExistentId)).thenThrow(new EmployeeNotFoundException(nonExistentId));

        // When & Then
        mockMvc.perform(get("/api/v1/employee/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Employee not found"));
    }

    @Test
    @DisplayName("GET /api/v1/employee/highestSalary should return highest salary")
    void getHighestSalaryOfEmployees_ShouldReturnHighestSalary() throws Exception {
        // Given
        Integer highestSalary = 95000;
        when(employeeServiceImpl.getHighestSalary()).thenReturn(highestSalary);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(highestSalary.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/employee/topTenHighestEarningEmployeeNames should return top earners")
    void getTopTenHighestEarningEmployeeNames_ShouldReturnTopEarners() throws Exception {
        // Given
        List<String> topEarners = Arrays.asList("Jane Smith", "John Doe");
        when(employeeServiceImpl.getTopTenHighestEarningEmployeeNames()).thenReturn(topEarners);

        // When & Then
        mockMvc.perform(get("/api/v1/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("Jane Smith"))
                .andExpect(jsonPath("$[1]").value("John Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/employee should create new employee")
    void createEmployee_ShouldCreateEmployee() throws Exception {
        // Given
        Employee createdEmployee = Employee.builder()
                .id(UUID.randomUUID().toString())
                .name(createEmployeeInput.getName())
                .salary(createEmployeeInput.getSalary())
                .age(createEmployeeInput.getAge())
                .title(createEmployeeInput.getTitle())
                .email(createEmployeeInput.getEmail())
                .build();

        when(employeeServiceImpl.createEmployee(any(CreateEmployeeInput.class))).thenReturn(createdEmployee);

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createEmployeeInput)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.employee_name").value("New Employee"))
                .andExpect(jsonPath("$.employee_salary").value(70000));
    }

    @Test
    @DisplayName("POST /api/v1/employee should return 400 for invalid input")
    void createEmployee_WithInvalidInput_ShouldReturn400() throws Exception {
        // Given
        CreateEmployeeInput invalidInput = CreateEmployeeInput.builder()
                .name("") // Invalid empty name
                .salary(-1000) // Invalid negative salary
                .age(10) // Invalid age below minimum
                .title("")
                .email("invalid-email")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    @DisplayName("DELETE /api/v1/employee/{id} should delete employee")
    void deleteEmployeeById_ShouldDeleteEmployee() throws Exception {
        // Given
        String employeeId = employee1.getId();
        when(employeeServiceImpl.deleteEmployeeById(employeeId)).thenReturn("Employee deleted successfully");

        // When & Then
        mockMvc.perform(delete("/api/v1/employee/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("Employee deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/v1/employee/{id} should return 404 when employee not found")
    void deleteEmployeeById_WhenEmployeeNotFound_ShouldReturn404() throws Exception {
        // Given
        String nonExistentId = "non-existent-id";
        when(employeeServiceImpl.deleteEmployeeById(nonExistentId)).thenThrow(new EmployeeNotFoundException(nonExistentId));

        // When & Then
        mockMvc.perform(delete("/api/v1/employee/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Employee not found"));
    }

    @Test
    @DisplayName("Should return 500 for service exceptions")
    void shouldReturn500ForServiceExceptions() throws Exception {
        // Given
        when(employeeServiceImpl.getAllEmployees()).thenThrow(new EmployeeServiceException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Service error"));
    }
}
