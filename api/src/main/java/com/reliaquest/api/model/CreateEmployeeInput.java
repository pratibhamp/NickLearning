package com.reliaquest.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class holds the information needed to add a new employee.
 * Clients use this when they want to add an employee. It is different from the main Employee class because creating and updating have different rules. There is no ID when creating.
 * The age must be between 16 and 75 because most jobs require employees to be at least 16, and 75 is a common maximum age for working.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeInput {

    @NotBlank(message = "Employee name is required")
    private String name;

    @NotNull(message = "Employee salary is required") @Positive(message = "Employee salary must be positive") private Integer salary;

    @NotNull(message = "Employee age is required") @Min(value = 16, message = "Employee age must be at least 16")
    @Max(value = 75, message = "Employee age must not exceed 75")
    private Integer age;

    @NotBlank(message = "Employee title is required")
    private String title;

    @Email(message = "Employee email must be valid")
    private String email;
}
