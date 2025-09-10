package com.reliaquest.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reliaquest.api.client.MockEmployeeApiClient;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.server.model.MockEmployee;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Service Tests")
class EmployeeServiceImplTest {

    @Mock
    private MockEmployeeApiClient mockEmployeeApiClient;

    @InjectMocks
    private EmployeeServiceImpl employeeServiceImpl;

    private MockEmployee mockEmployee1;
    private MockEmployee mockEmployee2;
    private MockEmployee mockEmployee3;

    @BeforeEach
    void setUp() {
        mockEmployee1 = MockEmployee.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .salary(75000)
                .age(30)
                .title("Software Engineer")
                .email("john.doe@example.com")
                .build();

        mockEmployee2 = MockEmployee.builder()
                .id(UUID.randomUUID())
                .name("Jane Smith")
                .salary(85000)
                .age(28)
                .title("Senior Software Engineer")
                .email("jane.smith@example.com")
                .build();

        mockEmployee3 = MockEmployee.builder()
                .id(UUID.randomUUID())
                .name("Bob Johnson")
                .salary(95000)
                .age(35)
                .title("Tech Lead")
                .email("bob.johnson@example.com")
                .build();
    }

    @Test
    @DisplayName("Should get all employees successfully")
    void getAllEmployees_ShouldReturnAllEmployees() {
        // Given
        List<MockEmployee> mockEmployees = Arrays.asList(mockEmployee1, mockEmployee2, mockEmployee3);
        when(mockEmployeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        // When
        List<Employee> result = employeeServiceImpl.getAllEmployees();

        // Then
        assertEquals(3, result.size());
        assertEquals("John Doe", result.get(0).getName());
        assertEquals("Jane Smith", result.get(1).getName());
        assertEquals("Bob Johnson", result.get(2).getName());
        verify(mockEmployeeApiClient).getAllEmployees();
    }

    @Test
    @DisplayName("Should search employees by name successfully")
    void searchEmployeesByName_ShouldReturnMatchingEmployees() {
        // Given
        List<MockEmployee> allEmployees = Arrays.asList(mockEmployee1, mockEmployee2, mockEmployee3);
        when(mockEmployeeApiClient.getAllEmployees()).thenReturn(allEmployees);

        // When
        List<Employee> result = employeeServiceImpl.searchEmployeesByName("John");

        // Then
        assertEquals(2, result.size()); // John Doe and Bob Johnson
        verify(mockEmployeeApiClient).getAllEmployees();
    }

    @Test
    @DisplayName("Should throw exception for empty search string")
    void searchEmployeesByName_WithEmptyString_ShouldThrowException() {
        // When & Then
        assertThrows(EmployeeServiceException.class, () -> employeeServiceImpl.searchEmployeesByName(""));
    }

    @Test
    @DisplayName("Should get employee by ID successfully")
    void getEmployeeById_ShouldReturnEmployee() {
        // Given
        String employeeId = mockEmployee1.getId().toString();
        when(mockEmployeeApiClient.getEmployeeById(employeeId)).thenReturn(mockEmployee1);

        // When
        Employee result = employeeServiceImpl.getEmployeeById(employeeId);

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals(75000, result.getSalary());
        verify(mockEmployeeApiClient).getEmployeeById(employeeId);
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when employee not found")
    void getEmployeeById_WithNonExistentId_ShouldThrowEmployeeNotFoundException() {
        // Given
        String nonExistentId = "non-existent-id";
        when(mockEmployeeApiClient.getEmployeeById(nonExistentId)).thenReturn(null);

        // When & Then
        assertThrows(EmployeeNotFoundException.class, () -> employeeServiceImpl.getEmployeeById(nonExistentId));
    }

    @Test
    @DisplayName("Should get highest salary successfully")
    void getHighestSalary_ShouldReturnHighestSalary() {
        // Given
        List<MockEmployee> mockEmployees = Arrays.asList(mockEmployee1, mockEmployee2, mockEmployee3);
        when(mockEmployeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        // When
        Integer result = employeeServiceImpl.getHighestSalary();

        // Then
        assertEquals(95000, result);
        verify(mockEmployeeApiClient).getAllEmployees();
    }

    @Test
    @DisplayName("Should get top ten highest earning employee names successfully")
    void getTopTenHighestEarningEmployeeNames_ShouldReturnTopEarners() {
        // Given
        List<MockEmployee> mockEmployees = Arrays.asList(mockEmployee1, mockEmployee2, mockEmployee3);
        when(mockEmployeeApiClient.getAllEmployees()).thenReturn(mockEmployees);

        // When
        List<String> result = employeeServiceImpl.getTopTenHighestEarningEmployeeNames();

        // Then
        assertEquals(3, result.size());
        assertEquals("Bob Johnson", result.get(0)); // Highest salary
        assertEquals("Jane Smith", result.get(1)); // Second highest
        assertEquals("John Doe", result.get(2)); // Lowest
        verify(mockEmployeeApiClient).getAllEmployees();
    }

    @Test
    @DisplayName("Should create employee successfully")
    void createEmployee_ShouldReturnCreatedEmployee() {
        // Given
        CreateEmployeeInput input = CreateEmployeeInput.builder()
                .name("New Employee")
                .salary(70000)
                .age(25)
                .title("Junior Developer")
                .email("new.employee@example.com")
                .build();

        MockEmployee createdMockEmployee = MockEmployee.builder()
                .id(UUID.randomUUID())
                .name(input.getName())
                .salary(input.getSalary())
                .age(input.getAge())
                .title(input.getTitle())
                .email(input.getEmail())
                .build();

        when(mockEmployeeApiClient.createEmployee(input)).thenReturn(createdMockEmployee);

        // When
        Employee result = employeeServiceImpl.createEmployee(input);

        // Then
        assertNotNull(result);
        assertEquals("New Employee", result.getName());
        assertEquals(70000, result.getSalary());
        verify(mockEmployeeApiClient).createEmployee(input);
    }

    @Test
    @DisplayName("Should throw exception when creating employee with null input")
    void createEmployee_WithNullInput_ShouldThrowException() {
        // When & Then
        assertThrows(EmployeeServiceException.class, () -> employeeServiceImpl.createEmployee(null));
    }

    @Test
    @DisplayName("Should delete employee successfully")
    void deleteEmployeeById_ShouldReturnSuccessMessage() {
        // Given
        String employeeId = mockEmployee1.getId().toString();
        when(mockEmployeeApiClient.getEmployeeById(employeeId)).thenReturn(mockEmployee1);
        when(mockEmployeeApiClient.deleteEmployee(employeeId)).thenReturn(true);

        // When
        String result = employeeServiceImpl.deleteEmployeeById(employeeId);

        // Then
        assertEquals("Employee deleted successfully", result);
        verify(mockEmployeeApiClient).getEmployeeById(employeeId);
        verify(mockEmployeeApiClient).deleteEmployee(employeeId);
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when deleting non-existent employee")
    void deleteEmployeeById_WithNonExistentId_ShouldThrowEmployeeNotFoundException() {
        // Given
        String nonExistentId = "non-existent-id";
        when(mockEmployeeApiClient.getEmployeeById(nonExistentId)).thenReturn(null);

        // When & Then
        assertThrows(EmployeeNotFoundException.class, () -> employeeServiceImpl.deleteEmployeeById(nonExistentId));
    }

    @Test
    @DisplayName("Should throw exception when delete operation fails")
    void deleteEmployeeById_WhenDeleteFails_ShouldThrowException() {
        // Given
        String employeeId = mockEmployee1.getId().toString();
        when(mockEmployeeApiClient.getEmployeeById(employeeId)).thenReturn(mockEmployee1);
        when(mockEmployeeApiClient.deleteEmployee(employeeId)).thenReturn(false);

        // When & Then
        assertThrows(EmployeeServiceException.class, () -> employeeServiceImpl.deleteEmployeeById(employeeId));
    }
}
