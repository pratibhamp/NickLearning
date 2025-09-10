package com.reliaquest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * This class stores information about an employee.
 * This is the main employee data structure. The JSON property names are different (like employee_name instead of name) because the external API needs them that way. The @JsonProperty annotation matches our field names to the external format.
 * The validation annotations make sure we only accept correct data, like positive salaries and ages between 16 and 75.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @JsonProperty("id")
    private String id;

    @NotBlank(message = "Employee name is required")
    @JsonProperty("employee_name")
    private String name;

    @NotNull(message = "Employee salary is required") @Positive(message = "Employee salary must be positive") @JsonProperty("employee_salary")
    private Integer salary;

    @NotNull(message = "Employee age is required") @Min(value = 16, message = "Employee age must be at least 16")
    @Max(value = 75, message = "Employee age must not exceed 75")
    @JsonProperty("employee_age")
    private Integer age;

    @NotBlank(message = "Employee title is required")
    @JsonProperty("employee_title")
    private String title;

    @Email(message = "Employee email must be valid")
    @JsonProperty("employee_email")
    private String email;
}
