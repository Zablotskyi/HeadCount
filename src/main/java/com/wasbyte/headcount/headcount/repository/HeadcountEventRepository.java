package com.wasbyte.headcount.headcount.repository;

import com.wasbyte.headcount.headcount.entity.HeadcountEvent;
import com.wasbyte.headcount.headcount.entity.HeadcountEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface HeadcountEventRepository extends JpaRepository<HeadcountEvent, Long> {

    @Override
    @EntityGraph(attributePaths = {"scopeOrganizationUnit", "startedBy", "closedBy", "cancelledBy"})
    Optional<HeadcountEvent> findById(Long id);

    @EntityGraph(attributePaths = "scopeOrganizationUnit")
    List<HeadcountEvent> findByStatus(HeadcountEventStatus status);

    @EntityGraph(attributePaths = {"scopeOrganizationUnit", "startedBy", "closedBy", "cancelledBy"})
    Optional<HeadcountEvent> findFirstByStatusOrderByStartedAtDesc(HeadcountEventStatus status);

    @EntityGraph(attributePaths = {"scopeOrganizationUnit", "startedBy", "closedBy", "cancelledBy"})
    Optional<HeadcountEvent> findFirstByStatusAndScopeOrganizationUnitIdOrderByStartedAtDesc(
            HeadcountEventStatus status, Long scopeOrganizationUnitId);

    boolean existsByScopeOrganizationUnitIdAndStatus(Long organizationUnitId, HeadcountEventStatus status);
}
