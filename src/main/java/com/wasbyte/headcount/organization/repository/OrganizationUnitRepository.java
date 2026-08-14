package com.wasbyte.headcount.organization.repository;

import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Long> {

    List<OrganizationUnit> findByParentId(Long parentId);

    List<OrganizationUnit> findByParentIsNullOrderBySortOrderAscNameAsc();
}
