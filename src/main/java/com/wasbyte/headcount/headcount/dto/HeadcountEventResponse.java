package com.wasbyte.headcount.headcount.dto;

import com.wasbyte.headcount.headcount.entity.HeadcountEventStatus;

import java.time.LocalDateTime;

public record HeadcountEventResponse(
        Long id, String title, String description, HeadcountEventStatus status,
        Long scopeOrganizationUnitId, String scopeOrganizationUnitName,
        LocalDateTime startedAt, Long startedById, String startedByName,
        LocalDateTime closedAt, Long closedById,
        LocalDateTime cancelledAt, Long cancelledById,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
