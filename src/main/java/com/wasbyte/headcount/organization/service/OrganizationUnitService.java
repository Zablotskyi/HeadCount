package com.wasbyte.headcount.organization.service;

import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.exception.ResourceNotFoundException;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.repository.OrganizationUnitRepository;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrganizationUnitService {

    private final OrganizationUnitRepository organizationUnitRepository;
    private final UserRepository userRepository;

    public OrganizationUnitService(OrganizationUnitRepository organizationUnitRepository,
                                   UserRepository userRepository) {
        this.organizationUnitRepository = organizationUnitRepository;
        this.userRepository = userRepository;
    }

    public OrganizationUnit findById(Long id) {
        return organizationUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization unit not found: " + id));
    }

    public List<OrganizationUnit> getChildren(Long parentId) {
        findById(parentId);
        return organizationUnitRepository.findByParentId(parentId);
    }

    public List<OrganizationUnit> getRoots() {
        return organizationUnitRepository.findByParentIsNullOrderBySortOrderAscNameAsc();
    }

    @Transactional
    public OrganizationUnit create(OrganizationUnit unit) {
        if (unit.getId() != null) {
            throw new InvalidOperationException("A new organization unit must not already have an id");
        }
        if (unit.getParent() != null) {
            unit.setParent(findById(unit.getParent().getId()));
        }
        LocalDateTime now = LocalDateTime.now();
        unit.setCreatedAt(now);
        unit.setUpdatedAt(now);
        return organizationUnitRepository.save(unit);
    }

    @Transactional
    public OrganizationUnit update(Long unitId, OrganizationUnit changes) {
        OrganizationUnit unit = findById(unitId);
        unit.setName(changes.getName());
        unit.setCode(changes.getCode());
        unit.setType(changes.getType());
        unit.setSortOrder(changes.getSortOrder());
        unit.setUpdatedAt(LocalDateTime.now());
        return unit;
    }

    @Transactional
    public OrganizationUnit changeParent(Long unitId, Long parentId) {
        OrganizationUnit unit = findById(unitId);
        OrganizationUnit parent = parentId == null ? null : findById(parentId);
        validateParent(unit, parent);
        unit.setParent(parent);
        unit.setUpdatedAt(LocalDateTime.now());
        return unit;
    }

    @Transactional
    public OrganizationUnit assignManager(Long unitId, Long managerId) {
        OrganizationUnit unit = findById(unitId);
        User manager = managerId == null ? null : userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + managerId));
        unit.setManager(manager);
        unit.setUpdatedAt(LocalDateTime.now());
        return unit;
    }

    @Transactional
    public OrganizationUnit setActive(Long unitId, boolean active) {
        OrganizationUnit unit = findById(unitId);
        unit.setActive(active);
        unit.setUpdatedAt(LocalDateTime.now());
        return unit;
    }

    private void validateParent(OrganizationUnit unit, OrganizationUnit candidateParent) {
        if (candidateParent == null) {
            return;
        }
        if (unit.getId().equals(candidateParent.getId())) {
            throw new InvalidOperationException("An organization unit cannot be its own parent");
        }

        Set<Long> visited = new HashSet<>();
        OrganizationUnit current = candidateParent;
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new InvalidOperationException("The organization hierarchy already contains a cycle");
            }
            if (unit.getId().equals(current.getId())) {
                throw new InvalidOperationException("Changing the parent would create an organization hierarchy cycle");
            }
            current = current.getParent();
        }
    }
}
