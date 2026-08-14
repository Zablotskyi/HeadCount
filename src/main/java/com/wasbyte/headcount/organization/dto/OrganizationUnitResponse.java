package com.wasbyte.headcount.organization.dto;

import com.wasbyte.headcount.organization.entity.OrganizationUnitType;

public record OrganizationUnitResponse(
        Long id, String name, String code, OrganizationUnitType type,
        Long parentId, Long managerId, boolean active, int sortOrder) {
}
