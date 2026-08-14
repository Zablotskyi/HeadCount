package com.wasbyte.headcount.organization.dto;

import com.wasbyte.headcount.organization.entity.OrganizationUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationUnitRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String code,
        @NotNull OrganizationUnitType type,
        @NotNull Integer sortOrder) {
}
