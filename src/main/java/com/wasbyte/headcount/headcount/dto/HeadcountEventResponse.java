package com.wasbyte.headcount.headcount.dto;

import com.wasbyte.headcount.headcount.entity.HeadcountEventStatus;

import java.time.Instant;

public record HeadcountEventResponse(
        Long id, String title, String description, HeadcountEventStatus status,
        Long scopeOrganizationUnitId, String scopeOrganizationUnitName,
        Instant startedAt, Long startedById, String startedByName,
        Instant closedAt, Long closedById,
        Instant cancelledAt, Long cancelledById,
        Instant createdAt, Instant updatedAt) {
}
