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
 * Input data for creating a new employee.
 *
 * This is what clients send when they want to create a new employee. I've
 * kept it separate from the main Employee class because create operations
 * often have different validation rules than updates (and we don't have
 * an ID yet when creating).
 *
 * The age limits might seem arbitrary, but they're based on typical
 * employment law requirements - you need to be at least 16 to work in
 * most places, and 75 is a reasonable upper bound for active employment.
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
