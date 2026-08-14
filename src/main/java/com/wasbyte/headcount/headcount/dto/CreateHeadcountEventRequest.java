package com.wasbyte.headcount.headcount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHeadcountEventRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull Long scopeOrganizationUnitId) {
}
