package com.wasbyte.headcount.organization.controller;

import com.wasbyte.headcount.common.dto.SetActiveRequest;
import com.wasbyte.headcount.organization.dto.AssignManagerRequest;
import com.wasbyte.headcount.organization.dto.ChangeParentRequest;
import com.wasbyte.headcount.organization.dto.CreateOrganizationUnitRequest;
import com.wasbyte.headcount.organization.dto.OrganizationUnitResponse;
import com.wasbyte.headcount.organization.dto.UpdateOrganizationUnitRequest;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.mapper.OrganizationUnitMapper;
import com.wasbyte.headcount.organization.service.OrganizationUnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organization-units")
public class OrganizationUnitController {

    private static final String MANAGEMENT_ROLES = "hasAnyRole('COUNTRY_MANAGER', 'REGIONAL_MANAGER', "
            + "'SUPPORT_MANAGER', 'PROGRAM_MANAGER', 'DEPARTMENT_MANAGER', 'UNIT_MANAGER', "
            + "'SECURITY_OFFICER', 'SECURITY_MANAGER', 'ADMIN')";

    private final OrganizationUnitService service;
    private final OrganizationUnitMapper mapper;

    public OrganizationUnitController(OrganizationUnitService service, OrganizationUnitMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public OrganizationUnitResponse getById(@PathVariable Long id) {
        return mapper.toResponse(service.findById(id));
    }

    @GetMapping("/roots")
    public List<OrganizationUnitResponse> getRoots() {
        return service.getRoots().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}/children")
    public List<OrganizationUnitResponse> getChildren(@PathVariable Long id) {
        return service.getChildren(id).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGEMENT_ROLES)
    public OrganizationUnitResponse create(@Valid @RequestBody CreateOrganizationUnitRequest request) {
        OrganizationUnit unit = mapper.toEntity(request);
        if (request.parentId() != null) {
            unit.setParent(service.findById(request.parentId()));
        }
        return mapper.toResponse(service.create(unit));
    }

    @PutMapping("/{id}")
    @PreAuthorize(MANAGEMENT_ROLES)
    public OrganizationUnitResponse update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateOrganizationUnitRequest request) {
        return mapper.toResponse(service.update(id, mapper.toEntity(request)));
    }

    @PatchMapping("/{id}/parent")
    @PreAuthorize(MANAGEMENT_ROLES)
    public OrganizationUnitResponse changeParent(@PathVariable Long id,
                                                 @RequestBody ChangeParentRequest request) {
        return mapper.toResponse(service.changeParent(id, request.parentId()));
    }

    @PatchMapping("/{id}/manager")
    @PreAuthorize(MANAGEMENT_ROLES)
    public OrganizationUnitResponse assignManager(@PathVariable Long id,
                                                  @RequestBody AssignManagerRequest request) {
        return mapper.toResponse(service.assignManager(id, request.managerId()));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize(MANAGEMENT_ROLES)
    public OrganizationUnitResponse setActive(@PathVariable Long id,
                                              @Valid @RequestBody SetActiveRequest request) {
        return mapper.toResponse(service.setActive(id, request.active()));
    }
}
