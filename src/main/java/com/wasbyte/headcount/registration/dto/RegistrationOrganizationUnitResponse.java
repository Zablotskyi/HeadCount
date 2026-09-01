package com.wasbyte.headcount.registration.dto;

import com.wasbyte.headcount.organization.entity.OrganizationUnitType;

public record RegistrationOrganizationUnitResponse(
        Long id, String name, String code, OrganizationUnitType type, Long parentId, int depth) {
}
