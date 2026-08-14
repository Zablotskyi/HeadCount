package com.wasbyte.headcount.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 100) String resourceNumber,
        @Size(max = 100) String grade,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 50) String mobileNumber,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @Size(max = 100) String country,
        @Size(max = 100) String city,
        @Size(max = 150) String office,
        Long organizationUnitId,
        @Size(max = 150) String position,
        Long lineManagerId,
        @Size(max = 500) String address,
        @Size(max = 50) String authorizedPersonPhoneNumber,
        @NotBlank @Size(max = 100) String timeZone) {
}
