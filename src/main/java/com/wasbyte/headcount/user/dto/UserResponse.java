package com.wasbyte.headcount.user.dto;

import com.wasbyte.headcount.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id, String username, String resourceNumber, String grade,
        String firstName, String lastName, String mobileNumber, String email,
        String country, String city, String office,
        Long organizationUnitId, String organizationUnitName,
        String position, Long lineManagerId, String lineManagerName,
        String address, String authorizedPersonPhoneNumber, String timeZone,
        UserStatus status, boolean enabled, boolean emailVerified,
        Set<String> roles, LocalDateTime lastLoginAt,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
