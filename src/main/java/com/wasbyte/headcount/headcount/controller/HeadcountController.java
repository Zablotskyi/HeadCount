package com.wasbyte.headcount.headcount.controller;

import com.wasbyte.headcount.headcount.dto.ConfirmNeedHelpRequest;
import com.wasbyte.headcount.headcount.dto.ConfirmSafeRequest;
import com.wasbyte.headcount.headcount.dto.CreateHeadcountEventRequest;
import com.wasbyte.headcount.headcount.dto.HeadcountEventResponse;
import com.wasbyte.headcount.headcount.dto.HeadcountParticipantResponse;
import com.wasbyte.headcount.headcount.mapper.HeadcountMapper;
import com.wasbyte.headcount.headcount.service.HeadcountService;
import com.wasbyte.headcount.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/headcount/events")
public class HeadcountController {

    private static final String MANAGEMENT_ROLES = "hasAnyRole('COUNTRY_MANAGER', 'REGIONAL_MANAGER', "
            + "'SUPPORT_MANAGER', 'PROGRAM_MANAGER', 'DEPARTMENT_MANAGER', 'UNIT_MANAGER', "
            + "'SECURITY_OFFICER', 'SECURITY_MANAGER', 'ADMIN')";

    private final HeadcountService service;
    private final HeadcountMapper mapper;

    public HeadcountController(HeadcountService service, HeadcountMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGEMENT_ROLES)
    public HeadcountEventResponse create(@Valid @RequestBody CreateHeadcountEventRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return mapper.toResponse(service.createEvent(
                request.title(), request.description(), request.scopeOrganizationUnitId(), principal.getUserId()));
    }

    @GetMapping("/{eventId}")
    public HeadcountEventResponse getEvent(@PathVariable Long eventId) {
        return mapper.toResponse(service.findEventById(eventId));
    }

    @GetMapping("/{eventId}/participants")
    public List<HeadcountParticipantResponse> getParticipants(@PathVariable Long eventId) {
        return service.findParticipants(eventId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{eventId}/participants/{employeeId}/safe")
    public HeadcountParticipantResponse confirmSafe(
            @PathVariable Long eventId, @PathVariable Long employeeId,
            @Valid @RequestBody ConfirmSafeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return mapper.toResponse(service.confirmSafe(
                eventId, employeeId, principal.getUserId(), request.confirmationSource()));
    }

    @PostMapping("/{eventId}/participants/{employeeId}/need-help")
    public HeadcountParticipantResponse confirmNeedHelp(
            @PathVariable Long eventId, @PathVariable Long employeeId,
            @Valid @RequestBody ConfirmNeedHelpRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return mapper.toResponse(service.confirmNeedHelp(
                eventId, employeeId, principal.getUserId(),
                request.confirmationSource(), request.helpMessage()));
    }

    @PostMapping("/{eventId}/close")
    @PreAuthorize(MANAGEMENT_ROLES)
    public HeadcountEventResponse close(@PathVariable Long eventId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return mapper.toResponse(service.closeEvent(eventId, principal.getUserId()));
    }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize(MANAGEMENT_ROLES)
    public HeadcountEventResponse cancel(@PathVariable Long eventId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return mapper.toResponse(service.cancelEvent(eventId, principal.getUserId()));
    }
}
