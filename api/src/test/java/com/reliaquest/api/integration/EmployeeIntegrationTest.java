package com.reliaquest.api.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.reliaquest.api.model.CreateEmployeeInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {"mock.employee.api.base-url=http://localhost:8089", "logging.level.com.reliaquest.api=INFO"})
@DisplayName("Employee Integration Tests")
class EmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Integration test: GET all employees")
    void getAllEmployees_IntegrationTest() throws Exception {
        // Given
        String mockResponseBody =
                """
            {
                "data": [
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "employee_name": "John Doe",
                        "employee_salary": 75000,
                        "employee_age": 30,
                        "employee_title": "Software Engineer",
                        "employee_email": "john.doe@example.com"
                    },
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440001",
                        "employee_name": "Jane Smith",
                        "employee_salary": 85000,
                        "employee_age": 28,
                        "employee_title": "Senior Software Engineer",
                        "employee_email": "jane.smith@example.com"
                    }
                ],
                "status": "Successfully processed request."
            }
            """;

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponseBody)));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employee_name").value("John Doe"))
                .andExpect(jsonPath("$[0].employee_salary").value(75000))
                .andExpect(jsonPath("$[1].employee_name").value("Jane Smith"))
                .andExpect(jsonPath("$[1].employee_salary").value(85000));
    }

    @Test
    @DisplayName("Integration test: GET employee by ID")
    void getEmployeeById_IntegrationTest() throws Exception {
        // Given
        String employeeId = "550e8400-e29b-41d4-a716-446655440000";
        String mockResponseBody =
                """
            {
                "data": {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "employee_name": "John Doe",
                    "employee_salary": 75000,
                    "employee_age": 30,
                    "employee_title": "Software Engineer",
                    "employee_email": "john.doe@example.com"
                },
                "status": "Successfully processed request."
            }
            """;

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee/" + employeeId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponseBody)));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.employee_name").value("John Doe"))
                .andExpect(jsonPath("$.employee_salary").value(75000));
    }

    @Test
    @DisplayName("Integration test: POST create employee")
    void createEmployee_IntegrationTest() throws Exception {
        // Given
        CreateEmployeeInput input = CreateEmployeeInput.builder()
                .name("New Employee")
                .salary(70000)
                .age(25)
                .title("Junior Developer")
                .email("new.employee@example.com")
                .build();

        String mockResponseBody =
                """
            {
                "data": {
                    "id": "550e8400-e29b-41d4-a716-446655440002",
                    "employee_name": "New Employee",
                    "employee_salary": 70000,
                    "employee_age": 25,
                    "employee_title": "Junior Developer",
                    "employee_email": "new.employee@example.com"
                },
                "status": "Successfully processed request."
            }
            """;

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponseBody)));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.employee_name").value("New Employee"))
                .andExpect(jsonPath("$.employee_salary").value(70000))
                .andExpect(jsonPath("$.employee_age").value(25));
    }

    @Test
    @DisplayName("Integration test: DELETE employee by ID")
    void deleteEmployeeById_IntegrationTest() throws Exception {
        // Given
        String employeeId = "550e8400-e29b-41d4-a716-446655440000";

        // Mock GET request to fetch employee for deletion
        String getResponseBody =
                """
            {
                "data": {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "employee_name": "John Doe",
                    "employee_salary": 75000,
                    "employee_age": 30,
                    "employee_title": "Software Engineer",
                    "employee_email": "john.doe@example.com"
                },
                "status": "Successfully processed request."
            }
            """;

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee/" + employeeId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getResponseBody)));

        // Mock DELETE request
        String deleteResponseBody =
                """
            {
                "data": true,
                "status": "Successfully processed request."
            }
            """;

        wireMockServer.stubFor(delete(urlPathEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(deleteResponseBody)));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/employee/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("Employee deleted successfully"));
    }

    @Test
    @DisplayName("Integration test: Search employees by name")
    void searchEmployeesByName_IntegrationTest() throws Exception {
        // Given
        String searchString = "John";
        String mockResponseBody =
                """
            {
                "data": [
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "employee_name": "John Doe",
                        "employee_salary": 75000,
                        "employee_age": 30,
                        "employee_title": "Software Engineer",
                        "employee_email": "john.doe@example.com"
                    },
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440003",
                        "employee_name": "John Smith",
                        "employee_salary": 80000,
                        "employee_age": 32,
                        "employee_title": "Tech Lead",
                        "employee_email": "john.smith@example.com"
                    }
                ],
                "status": "Successfully processed request."
            }
            """;

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponseBody)));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/search/{searchString}", searchString))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employee_name").value("John Doe"))
                .andExpect(jsonPath("$[1].employee_name").value("John Smith"));
    }

    @Test
    @DisplayName("Integration test: GET highest salary")
    void getHighestSalary_IntegrationTest() throws Exception {
        // Given
        String mockResponseBody =
                """
            {
                "data": [
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "employee_name": "John Doe",
                        "employee_salary": 75000,
                        "employee_age": 30,
                        "employee_title": "Software Engineer",
                        "employee_email": "john.doe@example.com"
                    },
                    {
                        "id": "550e8400-e29b-41d4-a716-446655440001",
                        "employee_name": "Jane Smith",
                        "employee_salary": 95000,
                        "employee_age": 28,
                        "employee_title": "Senior Software Engineer",
                        "employee_email": "jane.smith@example.com"
                    }
                ],
                "status": "Successfully processed request."
            }
            """;

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponseBody)));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("95000"));
    }

    @Test
    @DisplayName("Integration test: External API failure handling")
    void externalApiFailure_ShouldReturn503() throws Exception {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/employee"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isServiceUnavailable());
    }
}
