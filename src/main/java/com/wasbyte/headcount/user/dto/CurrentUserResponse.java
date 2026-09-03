package com.wasbyte.headcount.user.dto;

import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String timeZone,
        Long organizationUnitId,
        Set<String> roles) {
}
