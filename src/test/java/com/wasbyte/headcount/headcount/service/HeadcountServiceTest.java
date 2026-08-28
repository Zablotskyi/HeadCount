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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
    }

    @Test
    void createEventCreatesActiveEvent() {
        arrangeEventCreation(List.of());

        HeadcountEvent event = service.createEvent("Alarm", "Description", 1L, 10L);

        assertSame(HeadcountEventStatus.ACTIVE, event.getStatus());
        assertSame(root, event.getScopeOrganizationUnit());
        assertSame(starter, event.getStartedBy());
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
    void confirmSafeChangesStatus() {
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE);
        arrangeConfirmation(participant);

        service.confirmSafe(1L, 20L, 10L, "SELF");

        assertSame(HeadcountParticipantStatus.SAFE, participant.getStatus());
        assertNotNull(participant.getConfirmedAt());
        assertSame(starter, participant.getConfirmedBy());
    }

    @Test
    void confirmNeedHelpSetsHelpDetails() {
        HeadcountParticipant participant = participant(HeadcountEventStatus.ACTIVE);
        arrangeConfirmation(participant);

        service.confirmNeedHelp(1L, 20L, 10L, "SELF", "Medical help");

        assertSame(HeadcountParticipantStatus.NEED_HELP, participant.getStatus());
        assertEquals("Medical help", participant.getHelpMessage());
        assertNotNull(participant.getHelpRequestedAt());
    }

    @Test
    void closeEventChangesStatus() {
        HeadcountEvent event = event(HeadcountEventStatus.ACTIVE);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(10L)).thenReturn(Optional.of(starter));

        service.closeEvent(1L, 10L);

        assertSame(HeadcountEventStatus.CLOSED, event.getStatus());
        assertNotNull(event.getClosedAt());
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

    private HeadcountParticipant participant(HeadcountEventStatus eventStatus) {
        HeadcountParticipant participant = new HeadcountParticipant();
        participant.setEvent(event(eventStatus));
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
        return user;
    }
}
