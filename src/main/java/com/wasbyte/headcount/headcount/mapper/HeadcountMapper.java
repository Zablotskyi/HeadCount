package com.wasbyte.headcount.headcount.mapper;

import com.wasbyte.headcount.headcount.dto.HeadcountEventResponse;
import com.wasbyte.headcount.headcount.dto.HeadcountParticipantResponse;
import com.wasbyte.headcount.headcount.dto.HeadcountParticipantDetailsResponse;
import com.wasbyte.headcount.headcount.entity.HeadcountEvent;
import com.wasbyte.headcount.headcount.entity.HeadcountParticipant;
import com.wasbyte.headcount.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class HeadcountMapper {

    public HeadcountEventResponse toResponse(HeadcountEvent event) {
        return new HeadcountEventResponse(
                event.getId(), event.getTitle(), event.getDescription(), event.getStatus(),
                event.getScopeOrganizationUnit().getId(), event.getScopeOrganizationUnit().getName(),
                event.getStartedAt(), event.getStartedBy().getId(), fullName(event.getStartedBy()),
                event.getClosedAt(), event.getClosedBy() == null ? null : event.getClosedBy().getId(),
                event.getCancelledAt(), event.getCancelledBy() == null ? null : event.getCancelledBy().getId(),
                event.getCreatedAt(), event.getUpdatedAt());
    }

    public HeadcountParticipantResponse toResponse(HeadcountParticipant participant) {
        return new HeadcountParticipantResponse(
                participant.getId(), participant.getEvent().getId(), participant.getEmployee().getId(),
                participant.getEmployeeNameSnapshot(), participant.getResourceNumberSnapshot(),
                participant.getOrganizationPathSnapshot(), participant.getStatus(),
                participant.getConfirmedAt(),
                participant.getConfirmedBy() == null ? null : participant.getConfirmedBy().getId(),
                participant.getConfirmationSource(), participant.getHelpMessage(),
                participant.getHelpRequestedAt(), participant.getVersion());
    }

    public HeadcountParticipantDetailsResponse toDetailsResponse(HeadcountParticipant participant) {
        User employee = participant.getEmployee();
        return new HeadcountParticipantDetailsResponse(
                participant.getId(), employee.getId(), employee.getFirstName(), employee.getLastName(),
                employee.getPosition(), employee.getEmail(), employee.getMobileNumber(),
                employee.getCountry(), employee.getCity(), employee.getOffice(), employee.getAddress());
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
