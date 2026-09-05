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
import com.wasbyte.headcount.user.entity.Role;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HeadcountServiceTest {

    @Mock HeadcountEventRepository eventRepository;
    @Mock HeadcountParticipantRepository participantRepository;
    @Mock OrganizationUnitRepository unitRepository;
    @Mock UserRepository userRepository;
    @InjectMocks HeadcountService service;

    private OrganizationUnit root;
    private User starter;

    @BeforeEach
    void setUp() {
        root = unit(1L, "Organization", null);
        starter = user(10L, "Starter", "User", "S-1", root);
        Role adminRole = role("ADMIN");
        when(starter.getRoles()).thenReturn(Set.of(adminRole));
    }

    @Test
    void createEventCreatesActiveEvent() {
        arrangeEventCreation(List.of());

        HeadcountEvent event = service.createEvent("Alarm", "Description", 1L, 10L);

        assertSame(HeadcountEventStatus.ACTIVE, event.getStatus());
        assertSame(root, event.getScopeOrganizationUnit());
        assertSame(starter, event.getStartedBy());
        assertNotNull(event.getStartedAt());
        assertNotNull(event.getCreatedAt());
        assertNotNull(event.getUpdatedAt());
        verify(eventRepository).save(event);
    }

    @Test
    void createEventIncludesUsersFromEntireScopeTree() {
        OrganizationUnit child = unit(2L, "Country", root);
        User rootUser = user(20L, "Root", "Employee", "R-20", root);
        User childUser = user(21L, "Child", "Employee", "R-21", child);
        arrangeEventCreation(List.of(rootUser, childUser));
        when(unitRepository.findByParentId(1L)).thenReturn(List.of(child));
        when(unitRepository.findByParentId(2L)).thenReturn(List.of());

        service.createEvent("Alarm", null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> scopeCaptor = ArgumentCaptor.forClass(Set.class);
        verify(userRepository).findByOrganizationUnitIdInAndEnabledTrue(scopeCaptor.capture());
        assertEquals(Set.of(1L, 2L), scopeCaptor.getValue());
        verify(participantRepository).saveAll(anyList());
    }

    @Test
    void newParticipantStartsPending() {
        User employee = user(20L, "Jane", "Smith", "R-20", root);
        arrangeEventCreation(List.of(employee));

        HeadcountParticipant participant = capturedCreatedParticipant();
        assertSame(HeadcountParticipantStatus.PENDING, participant.getStatus());
    }

    @Test
    void participantSnapshotsAreCreated() {
        OrganizationUnit child = unit(2L, "Ukraine", root);
        User employee = user(20L, "Jane", "Smith", "R-20", child);
        arrangeEventCreation(List.of(employee));
        when(unitRepository.findByParentId(1L)).thenReturn(List.of(child));
        when(unitRepository.findByParentId(2L)).thenReturn(List.of());

        HeadcountParticipant participant = capturedCreatedParticipant();
        assertEquals("Jane Smith", participant.getEmployeeNameSnapshot());
        assertEquals("R-20", participant.getResourceNumberSnapshot());
        assertEquals("Organization / Ukraine", participant.getOrganizationPathSnapshot());
    }

    @Test
    void participantDetailsAreLoadedOnlyByMatchingEventAndParticipantIds() {
        HeadcountParticipant participant = org.mockito.Mockito.mock(HeadcountParticipant.class);
        when(participantRepository.findByEventIdAndId(1L, 30L)).thenReturn(Optional.of(participant));

        assertSame(participant, service.findParticipantDetails(1L, 30L));
        verify(participantRepository).findByEventIdAndId(1L, 30L);
    }

    @Test
    void participantDetailsRejectParticipantFromAnotherEvent() {
        when(participantRepository.findByEventIdAndId(1L, 40L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.findParticipantDetails(1L, 40L));
    }

    @Test
    void participantDetailsRejectUnknownParticipant() {
        when(participantRepository.findByEventIdAndId(1L, 404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.findParticipantDetails(1L, 404L));
    }

    @Test
    void activeSummaryReturnsOnlyActiveEvents() {
        HeadcountEvent active = event(HeadcountEventStatus.ACTIVE);
        when(eventRepository.findByStatus(HeadcountEventStatus.ACTIVE)).thenReturn(List.of(active));

        List<HeadcountEvent> result = service.findActiveEvents();

        assertEquals(List.of(active), result);
        verify(eventRepository).findByStatus(HeadcountEventStatus.ACTIVE);
    }

    @Test
    void multipleActiveEventsReturned() {
        HeadcountEvent first = event(HeadcountEventStatus.ACTIVE);
        HeadcountEvent second = event(HeadcountEventStatus.ACTIVE);
        when(eventRepository.findByStatus(HeadcountEventStatus.ACTIVE))
                .thenReturn(List.of(first, second));

        assertEquals(List.of(first, second), service.findActiveEvents());
    }

    @Test
    void confirmSafeChangesStatus() {
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE);
        arrangeConfirmation(participant);

        service.confirmSafe(1L, 20L, 10L, "SELF");

        assertSame(HeadcountParticipantStatus.SAFE, participant.getStatus());
        assertNotNull(participant.getConfirmedAt());
        assertNotNull(participant.getUpdatedAt());
        assertSame(starter, participant.getConfirmedBy());
    }

    @Test
    void confirmNeedHelpSetsHelpDetails() {
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE);
        arrangeConfirmation(participant);

        service.confirmNeedHelp(1L, 20L, 10L, "SELF", "Medical help");

        assertSame(HeadcountParticipantStatus.NEED_HELP, participant.getStatus());
        assertSame(starter, participant.getConfirmedBy());
        assertEquals("Medical help", participant.getHelpMessage());
        assertNotNull(participant.getHelpRequestedAt());
        assertNotNull(participant.getConfirmedAt());
        assertNotNull(participant.getUpdatedAt());
    }

    @Test
    void employeeCanConfirmSelfSafe() {
        User employee = user(20L, "Self", "Employee", "R-20", root);
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE, employee);
        arrangeConfirmation(participant, employee, 20L);

        service.confirmSafe(1L, 20L, 20L, "SELF");

        assertSame(HeadcountParticipantStatus.SAFE, participant.getStatus());
        assertSame(employee, participant.getConfirmedBy());
    }

    @Test
    void employeeCanConfirmSelfNeedHelp() {
        User employee = user(20L, "Self", "Employee", "R-20", root);
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE, employee);
        arrangeConfirmation(participant, employee, 20L);

        service.confirmNeedHelp(1L, 20L, 20L, "SELF", "Help");

        assertSame(HeadcountParticipantStatus.NEED_HELP, participant.getStatus());
        assertSame(employee, participant.getConfirmedBy());
    }

    @Test
    void employeeCannotConfirmOther() {
        User employee = user(20L, "Other", "Employee", "R-20", root);
        User actor = user(30L, "Ordinary", "Employee", "R-30", root);
        arrangeConfirmation(participant(HeadcountEventStatus.ACTIVE, employee), actor, 30L);
        when(unitRepository.findByManagerId(30L)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.confirmSafe(1L, 20L, 30L, "WEB"));
    }

    @Test
    void unitManagerCanConfirmEmployeeInOwnUnit() {
        OrganizationUnit ict = unit(4L, "ICT", root);
        User employee = user(20L, "ICT", "Employee", "R-20", ict);
        User manager = user(30L, "ICT", "Manager", "R-30", root);
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE, employee);
        arrangeManagerBranch(participant, manager, ict, List.of());

        service.confirmSafe(1L, 20L, 30L, "WEB");

        assertSame(manager, participant.getConfirmedBy());
    }

    @Test
    void unitManagerCanConfirmEmployeeInDescendantUnit() {
        OrganizationUnit ict = unit(4L, "ICT", root);
        OrganizationUnit ictUnit = unit(5L, "ICT Unit", ict);
        User employee = user(20L, "Child", "Employee", "R-20", ictUnit);
        User manager = user(30L, "ICT", "Manager", "R-30", root);
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE, employee);
        arrangeManagerBranch(participant, manager, ict, List.of(ictUnit));
        when(unitRepository.findByParentId(5L)).thenReturn(List.of());

        service.confirmNeedHelp(1L, 20L, 30L, "WEB", "Help");

        assertSame(manager, participant.getConfirmedBy());
    }

    @Test
    void unitManagerCannotConfirmEmployeeInSiblingUnit() {
        OrganizationUnit office = unit(3L, "Office A", root);
        OrganizationUnit ict = unit(4L, "ICT", office);
        OrganizationUnit hr = unit(6L, "HR", office);
        User employee = user(20L, "HR", "Employee", "R-20", hr);
        User manager = user(30L, "ICT", "Manager", "R-30", ict);
        arrangeManagerBranch(participant(HeadcountEventStatus.ACTIVE, employee), manager, ict, List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.confirmSafe(1L, 20L, 30L, "WEB"));
    }

    @Test
    void unitManagerCannotConfirmEmployeeInUnrelatedBranch() {
        OrganizationUnit ict = unit(4L, "ICT", root);
        OrganizationUnit officeB = unit(7L, "Office B", root);
        User employee = user(20L, "Other", "Employee", "R-20", officeB);
        User manager = user(30L, "ICT", "Manager", "R-30", ict);
        arrangeManagerBranch(participant(HeadcountEventStatus.ACTIVE, employee), manager, ict, List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.confirmSafe(1L, 20L, 30L, "WEB"));
    }

    @Test
    void headcountManagerCanConfirmOtherParticipant() {
        assertGlobalRoleCanConfirmOther("HEADCOUNT_MANAGER");
    }

    @Test
    void adminCanConfirmOtherParticipant() {
        assertGlobalRoleCanConfirmOther("ADMIN");
    }

    @Test
    void employeeNotInEventCannotBeConfirmed() {
        when(participantRepository.findByEventIdAndEmployeeId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.confirmSafe(1L, 99L, 10L, "WEB"));
        verify(userRepository, never()).findById(10L);
    }

    @Test
    void closeEventChangesStatus() {
        HeadcountEvent event = event(HeadcountEventStatus.ACTIVE);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(10L)).thenReturn(Optional.of(starter));

        service.closeEvent(1L, 10L);

        assertSame(HeadcountEventStatus.CLOSED, event.getStatus());
        assertNotNull(event.getClosedAt());
        assertNotNull(event.getUpdatedAt());
        assertSame(starter, event.getClosedBy());
    }

    @Test
    void cancelEventChangesStatus() {
        HeadcountEvent event = event(HeadcountEventStatus.ACTIVE);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(10L)).thenReturn(Optional.of(starter));

        service.cancelEvent(1L, 10L);

        assertSame(HeadcountEventStatus.CANCELLED, event.getStatus());
        assertNotNull(event.getCancelledAt());
        assertNotNull(event.getUpdatedAt());
        assertSame(starter, event.getCancelledBy());
    }

    @Test
    void cannotChangeParticipantAfterClosed() {
        HeadcountParticipant participant = participant(HeadcountEventStatus.CLOSED);
        when(participantRepository.findByEventIdAndEmployeeId(1L, 20L)).thenReturn(Optional.of(participant));

        assertThrows(InvalidOperationException.class,
                () -> service.confirmSafe(1L, 20L, 10L, "SELF"));
        verify(userRepository, never()).findById(10L);
    }

    @Test
    void cannotChangeParticipantAfterCancelled() {
        HeadcountParticipant participant = participant(HeadcountEventStatus.CANCELLED);
        when(participantRepository.findByEventIdAndEmployeeId(1L, 20L)).thenReturn(Optional.of(participant));

        assertThrows(InvalidOperationException.class,
                () -> service.confirmNeedHelp(1L, 20L, 10L, "SELF", "Help"));
        verify(userRepository, never()).findById(10L);
    }

    @Test
    void duplicateActiveEventForScopeIsRejected() {
        when(unitRepository.findById(1L)).thenReturn(Optional.of(root));
        when(userRepository.findById(10L)).thenReturn(Optional.of(starter));
        when(eventRepository.existsByScopeOrganizationUnitIdAndStatus(1L, HeadcountEventStatus.ACTIVE))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.createEvent("Alarm", null, 1L, 10L));
        verify(eventRepository, never()).save(any());
    }

    private void arrangeEventCreation(List<User> employees) {
        when(unitRepository.findById(1L)).thenReturn(Optional.of(root));
        when(userRepository.findById(10L)).thenReturn(Optional.of(starter));
        when(unitRepository.findByParentId(1L)).thenReturn(List.of());
        when(userRepository.findByOrganizationUnitIdInAndEnabledTrue(any())).thenReturn(employees);
    }

    private HeadcountParticipant capturedCreatedParticipant() {
        service.createEvent("Alarm", null, 1L, 10L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HeadcountParticipant>> captor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).saveAll(captor.capture());
        return captor.getValue().getFirst();
    }

    private void arrangeConfirmation(HeadcountParticipant participant) {
        when(participantRepository.findByEventIdAndEmployeeId(1L, 20L)).thenReturn(Optional.of(participant));
        when(userRepository.findById(10L)).thenReturn(Optional.of(starter));
    }

    private void arrangeConfirmation(HeadcountParticipant participant, User actor, Long actorId) {
        when(participantRepository.findByEventIdAndEmployeeId(1L, 20L)).thenReturn(Optional.of(participant));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
    }

    private void arrangeManagerBranch(HeadcountParticipant participant, User manager,
                                      OrganizationUnit managedUnit, List<OrganizationUnit> children) {
        arrangeConfirmation(participant, manager, manager.getId());
        when(unitRepository.findByManagerId(manager.getId())).thenReturn(List.of(managedUnit));
        when(unitRepository.findByParentId(managedUnit.getId())).thenReturn(children);
    }

    private void assertGlobalRoleCanConfirmOther(String roleName) {
        User employee = user(20L, "Other", "Employee", "R-20", root);
        User actor = user(30L, "Authorized", "User", "R-30", root);
        Role role = role(roleName);
        when(actor.getRoles()).thenReturn(Set.of(role));
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE, employee);
        arrangeConfirmation(participant, actor, 30L);

        service.confirmSafe(1L, 20L, 30L, "WEB");

        assertSame(actor, participant.getConfirmedBy());
    }

    private HeadcountParticipant participant(HeadcountEventStatus eventStatus) {
        return participant(eventStatus, user(20L, "Participant", "User", "R-20", root));
    }

    private HeadcountParticipant participant(HeadcountEventStatus eventStatus, User employee) {
        HeadcountParticipant participant = new HeadcountParticipant();
        participant.setEvent(event(eventStatus));
        participant.setEmployee(employee);
        return participant;
    }

    private HeadcountEvent event(HeadcountEventStatus status) {
        HeadcountEvent event = new HeadcountEvent();
        event.setStatus(status);
        return event;
    }

    private OrganizationUnit unit(Long id, String name, OrganizationUnit parent) {
        OrganizationUnit unit = org.mockito.Mockito.mock(OrganizationUnit.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getName()).thenReturn(name);
        when(unit.getParent()).thenReturn(parent);
        return unit;
    }

    private User user(Long id, String firstName, String lastName,
                      String resourceNumber, OrganizationUnit unit) {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getLastName()).thenReturn(lastName);
        when(user.getResourceNumber()).thenReturn(resourceNumber);
        when(user.getOrganizationUnit()).thenReturn(unit);
        when(user.getRoles()).thenReturn(Set.of());
        return user;
    }

    private Role role(String name) {
        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getName()).thenReturn(name);
        return role;
    }
}
