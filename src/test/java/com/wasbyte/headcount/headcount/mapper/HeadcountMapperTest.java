package com.wasbyte.headcount.headcount.mapper;

import com.wasbyte.headcount.headcount.dto.HeadcountParticipantResponse;
import com.wasbyte.headcount.headcount.entity.HeadcountEvent;
import com.wasbyte.headcount.headcount.entity.HeadcountParticipant;
import com.wasbyte.headcount.headcount.entity.HeadcountParticipantStatus;
import com.wasbyte.headcount.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeadcountMapperTest {

    private final HeadcountMapper mapper = new HeadcountMapper();

    @Test
    void selfConfirmationContainsCorrectConfirmerId() {
        User employee = user(10L, "Oksana", "Polishchuk");
        HeadcountParticipantResponse response = mapper.toResponse(
                participant(employee, employee, HeadcountParticipantStatus.SAFE));

        assertEquals(10L, response.employeeId());
        assertEquals(10L, response.confirmedById());
        assertEquals("Oksana", response.confirmedByFirstName());
        assertEquals("Polishchuk", response.confirmedByLastName());
    }

    @Test
    void anotherUserNeedHelpConfirmationContainsConfirmerName() {
        User employee = user(10L, "Oksana", "Polishchuk");
        User confirmer = user(20L, "Andrii", "Ivanishev");
        HeadcountParticipantResponse response = mapper.toResponse(
                participant(employee, confirmer, HeadcountParticipantStatus.NEED_HELP));

        assertEquals(20L, response.confirmedById());
        assertEquals("Andrii", response.confirmedByFirstName());
        assertEquals("Ivanishev", response.confirmedByLastName());
    }

    @Test
    void pendingParticipantHasNoConfirmer() {
        HeadcountParticipantResponse response = mapper.toResponse(
                participant(user(10L, "Oksana", "Polishchuk"), null, HeadcountParticipantStatus.PENDING));

        assertNull(response.confirmedById());
        assertNull(response.confirmedByFirstName());
        assertNull(response.confirmedByLastName());
    }

    private HeadcountParticipant participant(User employee, User confirmedBy,
                                             HeadcountParticipantStatus status) {
        HeadcountEvent event = mock(HeadcountEvent.class);
        when(event.getId()).thenReturn(1L);
        HeadcountParticipant participant = mock(HeadcountParticipant.class);
        when(participant.getEvent()).thenReturn(event);
        when(participant.getEmployee()).thenReturn(employee);
        when(participant.getConfirmedBy()).thenReturn(confirmedBy);
        when(participant.getStatus()).thenReturn(status);
        return participant;
    }

    private User user(Long id, String firstName, String lastName) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFirstName()).thenReturn(firstName);
        when(user.getLastName()).thenReturn(lastName);
        return user;
    }
}
