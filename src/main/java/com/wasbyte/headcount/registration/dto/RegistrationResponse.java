package com.wasbyte.headcount.registration.dto;

import com.wasbyte.headcount.user.entity.UserStatus;

public record RegistrationResponse(Long id, String username, UserStatus status) {
}
