package com.wasbyte.headcount.headcount.repository;

import com.wasbyte.headcount.headcount.entity.HeadcountParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface HeadcountParticipantRepository extends JpaRepository<HeadcountParticipant, Long> {

    @EntityGraph(attributePaths = {"event", "employee", "confirmedBy"})
    List<HeadcountParticipant> findByEventId(Long eventId);

    @EntityGraph(attributePaths = {"event", "employee", "confirmedBy"})
    Optional<HeadcountParticipant> findByEventIdAndEmployeeId(Long eventId, Long employeeId);

    @EntityGraph(attributePaths = {"event", "employee"})
    Optional<HeadcountParticipant> findByEventIdAndId(Long eventId, Long participantId);
}
