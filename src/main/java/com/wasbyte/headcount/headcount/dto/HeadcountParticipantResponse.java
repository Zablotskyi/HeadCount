package com.wasbyte.headcount.headcount.dto;

import com.wasbyte.headcount.headcount.entity.HeadcountParticipantStatus;

import java.time.LocalDateTime;

public record HeadcountParticipantResponse(
        Long id, Long eventId, Long employeeId,
        String employeeFirstName, String employeeLastName, String position,
        Long organizationUnitId, Long lineManagerId,
        String employeeNameSnapshot, String resourceNumberSnapshot,
        String organizationPathSnapshot, HeadcountParticipantStatus status,
        LocalDateTime confirmedAt, Long confirmedById,
        String confirmedByFirstName, String confirmedByLastName, String confirmationSource,
        String helpMessage, LocalDateTime helpRequestedAt, long version) {
}
