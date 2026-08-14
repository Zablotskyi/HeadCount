package com.wasbyte.headcount.headcount.service;

import com.wasbyte.headcount.exception.DuplicateResourceException;
import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.exception.ResourceNotFoundException;
import com.wasbyte.headcount.headcount.entity.HeadcountEvent;
import com.wasbyte.headcount.headcount.entity.HeadcountEventStatus;
import com.wasbyte.headcount.headcount.entity.HeadcountParticipant;
import com.wasbyte.headcount.headcount.entity.HeadcountParticipantStatus;
import com.wasbyte.headcount.headcount.repository.HeadcountEventRepository;
import com.wasbyte.headcount.headcount.repository.HeadcountParticipantRepository;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.repository.OrganizationUnitRepository;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class HeadcountService {

    private final HeadcountEventRepository eventRepository;
    private final HeadcountParticipantRepository participantRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final UserRepository userRepository;

    public HeadcountService(HeadcountEventRepository eventRepository,
                            HeadcountParticipantRepository participantRepository,
                            OrganizationUnitRepository organizationUnitRepository,
                            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public HeadcountEvent createEvent(String title, String description,
                                      Long scopeOrganizationUnitId, Long startedById) {
        OrganizationUnit scope = findOrganizationUnit(scopeOrganizationUnitId);
        User startedBy = findUser(startedById);
        if (eventRepository.existsByScopeOrganizationUnitIdAndStatus(
                scopeOrganizationUnitId, HeadcountEventStatus.ACTIVE)) {
            throw new DuplicateResourceException("An active HeadCount event already exists for this scope");
        }

        LocalDateTime now = LocalDateTime.now();
        HeadcountEvent event = new HeadcountEvent();
        event.setTitle(title);
        event.setDescription(description);
        event.setStatus(HeadcountEventStatus.ACTIVE);
        event.setScopeOrganizationUnit(scope);
        event.setStartedAt(now);
        event.setStartedBy(startedBy);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        eventRepository.save(event);

        Set<Long> scopeIds = collectScopeIds(scope);
        List<User> employees = userRepository.findByOrganizationUnitIdInAndEnabledTrue(scopeIds);
        List<HeadcountParticipant> participants = new ArrayList<>(employees.size());
        for (User employee : employees) {
            participants.add(createParticipant(event, employee, now));
        }
        participantRepository.saveAll(participants);
        return event;
    }

    public List<HeadcountParticipant> findParticipants(Long eventId) {
        findEvent(eventId);
        return participantRepository.findByEventId(eventId);
    }

    public HeadcountEvent findEventById(Long eventId) {
        return findEvent(eventId);
    }

    public Optional<HeadcountEvent> findActiveEvent(Long scopeOrganizationUnitId) {
        if (scopeOrganizationUnitId == null) {
            return eventRepository.findFirstByStatusOrderByStartedAtDesc(HeadcountEventStatus.ACTIVE);
        }
        return eventRepository.findFirstByStatusAndScopeOrganizationUnitIdOrderByStartedAtDesc(
                HeadcountEventStatus.ACTIVE, scopeOrganizationUnitId);
    }

    @Transactional
    public HeadcountParticipant confirmSafe(Long eventId, Long employeeId,
                                            Long confirmedById, String confirmationSource) {
        HeadcountParticipant participant = findParticipant(eventId, employeeId);
        ensureEventActive(participant.getEvent());
        participant.setStatus(HeadcountParticipantStatus.SAFE);
        participant.setConfirmedAt(LocalDateTime.now());
        participant.setConfirmedBy(findUser(confirmedById));
        participant.setConfirmationSource(confirmationSource);
        participant.setUpdatedAt(LocalDateTime.now());
        return participant;
    }

    @Transactional
    public HeadcountParticipant confirmNeedHelp(Long eventId, Long employeeId,
                                                Long confirmedById, String confirmationSource,
                                                String helpMessage) {
        if (helpMessage == null || helpMessage.isBlank()) {
            throw new InvalidOperationException("A help message is required for NEED_HELP");
        }
        HeadcountParticipant participant = findParticipant(eventId, employeeId);
        ensureEventActive(participant.getEvent());
        LocalDateTime now = LocalDateTime.now();
        participant.setStatus(HeadcountParticipantStatus.NEED_HELP);
        participant.setConfirmedAt(now);
        participant.setConfirmedBy(findUser(confirmedById));
        participant.setConfirmationSource(confirmationSource);
        participant.setHelpMessage(helpMessage);
        participant.setHelpRequestedAt(now);
        participant.setUpdatedAt(now);
        return participant;
    }

    @Transactional
    public HeadcountEvent closeEvent(Long eventId, Long closedById) {
        HeadcountEvent event = findEvent(eventId);
        ensureEventActive(event);
        LocalDateTime now = LocalDateTime.now();
        event.setStatus(HeadcountEventStatus.CLOSED);
        event.setClosedAt(now);
        event.setClosedBy(findUser(closedById));
        event.setUpdatedAt(now);
        return event;
    }

    @Transactional
    public HeadcountEvent cancelEvent(Long eventId, Long cancelledById) {
        HeadcountEvent event = findEvent(eventId);
        ensureEventActive(event);
        LocalDateTime now = LocalDateTime.now();
        event.setStatus(HeadcountEventStatus.CANCELLED);
        event.setCancelledAt(now);
        event.setCancelledBy(findUser(cancelledById));
        event.setUpdatedAt(now);
        return event;
    }

    private HeadcountParticipant createParticipant(HeadcountEvent event, User employee, LocalDateTime now) {
        HeadcountParticipant participant = new HeadcountParticipant();
        participant.setEvent(event);
        participant.setEmployee(employee);
        participant.setEmployeeNameSnapshot(employee.getFirstName() + " " + employee.getLastName());
        participant.setResourceNumberSnapshot(employee.getResourceNumber());
        participant.setOrganizationPathSnapshot(buildOrganizationPath(employee.getOrganizationUnit()));
        participant.setStatus(HeadcountParticipantStatus.PENDING);
        participant.setCreatedAt(now);
        participant.setUpdatedAt(now);
        return participant;
    }

    private Set<Long> collectScopeIds(OrganizationUnit scope) {
        Set<Long> scopeIds = new HashSet<>();
        Deque<OrganizationUnit> pending = new ArrayDeque<>();
        pending.add(scope);
        while (!pending.isEmpty()) {
            OrganizationUnit unit = pending.removeFirst();
            if (!scopeIds.add(unit.getId())) {
                throw new InvalidOperationException("The organization hierarchy contains a cycle");
            }
            pending.addAll(organizationUnitRepository.findByParentId(unit.getId()));
        }
        return scopeIds;
    }

    private String buildOrganizationPath(OrganizationUnit unit) {
        if (unit == null) {
            return "";
        }
        List<String> names = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        OrganizationUnit current = unit;
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new InvalidOperationException("The organization hierarchy contains a cycle");
            }
            names.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(names);
        return String.join(" / ", names);
    }

    private HeadcountEvent findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("HeadCount event not found: " + eventId));
    }

    private HeadcountParticipant findParticipant(Long eventId, Long employeeId) {
        return participantRepository.findByEventIdAndEmployeeId(eventId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "HeadCount participant not found for event " + eventId + " and employee " + employeeId));
    }

    private OrganizationUnit findOrganizationUnit(Long id) {
        return organizationUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization unit not found: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private void ensureEventActive(HeadcountEvent event) {
        if (event.getStatus() != HeadcountEventStatus.ACTIVE) {
            throw new InvalidOperationException("Participants cannot be changed after an event is closed or cancelled");
        }
    }
}
