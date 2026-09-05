package com.wasbyte.headcount.headcount.dto;

import com.wasbyte.headcount.headcount.entity.HeadcountParticipantStatus;

import java.time.Instant;

public record HeadcountParticipantResponse(
        Long id, Long eventId, Long employeeId,
        String employeeFirstName, String employeeLastName, String position,
        Long organizationUnitId, Long lineManagerId,
        String employeeNameSnapshot, String resourceNumberSnapshot,
        String organizationPathSnapshot, HeadcountParticipantStatus status,
        Instant confirmedAt, Long confirmedById,
        String confirmedByFirstName, String confirmedByLastName, String confirmationSource,
        String helpMessage, Instant helpRequestedAt, long version) {
}
