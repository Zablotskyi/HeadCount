package com.wasbyte.headcount.headcount.repository;

import com.wasbyte.headcount.headcount.entity.HeadcountParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HeadcountParticipantRepository extends JpaRepository<HeadcountParticipant, Long> {

    List<HeadcountParticipant> findByEventId(Long eventId);

    Optional<HeadcountParticipant> findByEventIdAndEmployeeId(Long eventId, Long employeeId);
}
