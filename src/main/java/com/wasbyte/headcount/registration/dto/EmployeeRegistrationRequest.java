package com.wasbyte.headcount.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeRegistrationRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(min = 8, max = 128) String passwordConfirmation,
        @NotBlank @Size(max = 100) String resourceNumber,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 150) String position,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 50) String mobileNumber,
        @Size(max = 100) String country,
        @Size(max = 100) String city,
        @Size(max = 150) String office,
        @Size(max = 500) String address,
        @Size(max = 50) String authorizedPersonPhoneNumber,
        @NotBlank @Size(max = 100) String timeZone,
        @NotNull Long organizationUnitId) {
}
