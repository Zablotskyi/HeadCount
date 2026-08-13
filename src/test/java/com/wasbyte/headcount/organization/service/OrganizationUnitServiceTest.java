package com.wasbyte.headcount.organization.service;

import com.wasbyte.headcount.exception.InvalidOperationException;
import com.wasbyte.headcount.exception.ResourceNotFoundException;
import com.wasbyte.headcount.organization.entity.OrganizationUnit;
import com.wasbyte.headcount.organization.repository.OrganizationUnitRepository;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationUnitServiceTest {

    @Mock OrganizationUnitRepository unitRepository;
    @Mock UserRepository userRepository;
    @InjectMocks OrganizationUnitService service;

    @Test
    void findByIdReturnsUnit() {
        OrganizationUnit unit = mockUnit(1L, null);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));

        assertSame(unit, service.findById(1L));
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(unitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void changeParentRejectsSelfParent() {
        OrganizationUnit unit = mockUnit(1L, null);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));

        assertThrows(InvalidOperationException.class, () -> service.changeParent(1L, 1L));
    }

    @Test
    void changeParentRejectsHierarchyCycle() {
        OrganizationUnit unit = mockUnit(1L, null);
        OrganizationUnit child = mockUnit(2L, unit);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(unitRepository.findById(2L)).thenReturn(Optional.of(child));

        assertThrows(InvalidOperationException.class, () -> service.changeParent(1L, 2L));
    }

    @Test
    void changeParentUpdatesParent() {
        OrganizationUnit unit = mockUnit(1L, null);
        OrganizationUnit parent = mockUnit(2L, null);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(unitRepository.findById(2L)).thenReturn(Optional.of(parent));

        assertSame(unit, service.changeParent(1L, 2L));
        verify(unit).setParent(parent);
    }

    @Test
    void assignManagerUpdatesManager() {
        OrganizationUnit unit = mockUnit(1L, null);
        User manager = mock(User.class);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(userRepository.findById(9L)).thenReturn(Optional.of(manager));

        assertSame(unit, service.assignManager(1L, 9L));
        verify(unit).setManager(manager);
    }

    @Test
    void setActiveActivatesAndDeactivates() {
        OrganizationUnit unit = new OrganizationUnit();
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));

        service.setActive(1L, true);
        assertTrue(unit.isActive());
        service.setActive(1L, false);
        assertFalse(unit.isActive());
    }

    @Test
    void getChildrenReturnsRepositoryResult() {
        OrganizationUnit parent = mockUnit(1L, null);
        List<OrganizationUnit> children = List.of(mockUnit(2L, parent));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(unitRepository.findByParentId(1L)).thenReturn(children);

        assertEquals(children, service.getChildren(1L));
    }

    private OrganizationUnit mockUnit(Long id, OrganizationUnit parent) {
        OrganizationUnit unit = mock(OrganizationUnit.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getParent()).thenReturn(parent);
        return unit;
    }
}
