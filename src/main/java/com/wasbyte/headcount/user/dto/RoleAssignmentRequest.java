package com.wasbyte.headcount.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleAssignmentRequest(@NotBlank @Size(max = 100) String role) {
}
