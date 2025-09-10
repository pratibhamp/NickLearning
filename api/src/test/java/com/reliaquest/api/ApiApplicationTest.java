package com.reliaquest.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"mock.employee.api.base-url=http://localhost:8112"})
class ApiApplicationTest {

    @Test
    void contextLoads() {
        // Test that the Spring Boot application context loads successfully
    }
}
