package com.wasbyte.headcount.organization.mapper;

import com.wasbyte.headcount.organization.dto.CreateOrganizationUnitRequest;
import com.wasbyte.headcount.organization.dto.OrganizationUnitResponse;
import com.wasbyte.headcount.organization.dto.UpdateOrganizationUnitRequest;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import org.springframework.stereotype.Component;

@Component
public class OrganizationUnitMapper {

    public OrganizationUnitResponse toResponse(OrganizationUnit unit) {
        return new OrganizationUnitResponse(
                unit.getId(), unit.getName(), unit.getCode(), unit.getType(),
                unit.getParent() == null ? null : unit.getParent().getId(),
                unit.getManager() == null ? null : unit.getManager().getId(),
                unit.isActive(), unit.getSortOrder());
    }

    public OrganizationUnit toEntity(CreateOrganizationUnitRequest request) {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setName(request.name());
        unit.setCode(request.code());
        unit.setType(request.type());
        unit.setSortOrder(request.sortOrder());
        unit.setActive(true);
        return unit;
    }

    public OrganizationUnit toEntity(UpdateOrganizationUnitRequest request) {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setName(request.name());
        unit.setCode(request.code());
        unit.setType(request.type());
        unit.setSortOrder(request.sortOrder());
        return unit;
    }
}
