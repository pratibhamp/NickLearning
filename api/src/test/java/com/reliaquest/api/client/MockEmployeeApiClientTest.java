package com.reliaquest.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.reliaquest.api.exception.ExternalApiException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.server.model.MockEmployee;
import com.reliaquest.server.model.Response;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mock Employee API Client Tests")
class MockEmployeeApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MockEmployeeApiClient mockEmployeeApiClient;

    private MockEmployee mockEmployee1;
    private MockEmployee mockEmployee2;
    private CreateEmployeeInput createEmployeeInput;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mockEmployeeApiClient, "baseUrl", "http://localhost:8112");

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

        createEmployeeInput = CreateEmployeeInput.builder()
                .name("New Employee")
                .salary(70000)
                .age(25)
                .title("Junior Developer")
                .email("new.employee@example.com")
                .build();
    }

    @Test
    @DisplayName("Should get all employees successfully")
    void getAllEmployees_ShouldReturnAllEmployees() {
        // Given
        List<MockEmployee> mockEmployees = Arrays.asList(mockEmployee1, mockEmployee2);
        Response<List<MockEmployee>> response = Response.handledWith(mockEmployees);
        ResponseEntity<Response<List<MockEmployee>>> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee"),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // When
        List<MockEmployee> result = mockEmployeeApiClient.getAllEmployees();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).getName());
        assertEquals("Jane Smith", result.get(1).getName());
    }

    @Test
    @DisplayName("Should throw ExternalApiException when REST call fails")
    void getAllEmployees_WhenRestCallFails_ShouldThrowExternalApiException() {
        // Given
        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee"),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // When & Then
        assertThrows(ExternalApiException.class, () -> mockEmployeeApiClient.getAllEmployees());
    }

    @Test
    @DisplayName("Should get employee by ID successfully")
    void getEmployeeById_ShouldReturnEmployee() {
        // Given
        String employeeId = mockEmployee1.getId().toString();
        Response<MockEmployee> response = Response.handledWith(mockEmployee1);
        ResponseEntity<Response<MockEmployee>> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee/" + employeeId),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // When
        MockEmployee result = mockEmployeeApiClient.getEmployeeById(employeeId);

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals(75000, result.getSalary());
    }

    @Test
    @DisplayName("Should return null when employee not found")
    void getEmployeeById_WhenEmployeeNotFound_ShouldReturnNull() {
        // Given
        String employeeId = "non-existent-id";
        ResponseEntity<Response<MockEmployee>> responseEntity = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee/" + employeeId),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // When
        MockEmployee result = mockEmployeeApiClient.getEmployeeById(employeeId);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should create employee successfully")
    void createEmployee_ShouldReturnCreatedEmployee() {
        // Given
        MockEmployee createdEmployee = MockEmployee.builder()
                .id(UUID.randomUUID())
                .name(createEmployeeInput.getName())
                .salary(createEmployeeInput.getSalary())
                .age(createEmployeeInput.getAge())
                .title(createEmployeeInput.getTitle())
                .email(createEmployeeInput.getEmail())
                .build();

        Response<MockEmployee> response = Response.handledWith(createdEmployee);
        ResponseEntity<Response<MockEmployee>> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee"),
                        eq(HttpMethod.POST),
                        any(),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // When
        MockEmployee result = mockEmployeeApiClient.createEmployee(createEmployeeInput);

        // Then
        assertNotNull(result);
        assertEquals("New Employee", result.getName());
        assertEquals(70000, result.getSalary());
    }

    @Test
    @DisplayName("Should delete employee successfully")
    void deleteEmployee_ShouldReturnTrue() {
        // Given
        String employeeId = mockEmployee1.getId().toString();

        // Mock the GET request to fetch employee details
        Response<MockEmployee> getResponse = Response.handledWith(mockEmployee1);
        ResponseEntity<Response<MockEmployee>> getResponseEntity = new ResponseEntity<>(getResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee/" + employeeId),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(getResponseEntity);

        // Mock the DELETE request
        Response<Boolean> deleteResponse = Response.handledWith(true);
        ResponseEntity<Response<Boolean>> deleteResponseEntity = new ResponseEntity<>(deleteResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee"),
                        eq(HttpMethod.DELETE),
                        any(),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(deleteResponseEntity);

        // When
        boolean result = mockEmployeeApiClient.deleteEmployee(employeeId);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when employee to delete not found")
    void deleteEmployee_WhenEmployeeNotFound_ShouldReturnFalse() {
        // Given
        String employeeId = "non-existent-id";
        ResponseEntity<Response<MockEmployee>> responseEntity = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee/" + employeeId),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // When
        boolean result = mockEmployeeApiClient.deleteEmployee(employeeId);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw ExternalApiException when delete fails")
    void deleteEmployee_WhenDeleteFails_ShouldThrowExternalApiException() {
        // Given
        String employeeId = mockEmployee1.getId().toString();

        when(restTemplate.exchange(
                        eq("http://localhost:8112/api/v1/employee/" + employeeId),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // When & Then
        assertThrows(ExternalApiException.class, () -> mockEmployeeApiClient.deleteEmployee(employeeId));
    }
}
