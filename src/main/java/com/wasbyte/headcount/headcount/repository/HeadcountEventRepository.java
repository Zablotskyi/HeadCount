package com.wasbyte.headcount.headcount.repository;

import com.wasbyte.headcount.headcount.entity.HeadcountEvent;
import com.wasbyte.headcount.headcount.entity.HeadcountEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeadcountEventRepository extends JpaRepository<HeadcountEvent, Long> {

    List<HeadcountEvent> findByStatus(HeadcountEventStatus status);

    boolean existsByScopeOrganizationUnitIdAndStatus(Long organizationUnitId, HeadcountEventStatus status);
}
