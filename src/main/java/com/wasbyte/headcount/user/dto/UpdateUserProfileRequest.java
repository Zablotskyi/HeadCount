package com.wasbyte.headcount.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 100) String resourceNumber,
        @Size(max = 100) String grade,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 50) String mobileNumber,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 100) String country,
        @Size(max = 100) String city,
        @Size(max = 150) String office,
        @Size(max = 150) String position,
        @Size(max = 500) String address,
        @Size(max = 50) String authorizedPersonPhoneNumber,
        @NotBlank @Size(max = 100) String timeZone) {
}
