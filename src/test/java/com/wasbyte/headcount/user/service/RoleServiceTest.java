package com.wasbyte.headcount.user.service;

import com.wasbyte.headcount.user.entity.Role;
import com.wasbyte.headcount.user.entity.User;
import com.wasbyte.headcount.user.repository.RoleRepository;
import com.wasbyte.headcount.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wasbyte.headcount.exception.InvalidOperationException;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock RoleRepository roleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks RoleService service;

    @Test
    void findByNameReturnsRole() {
        Role role = new Role();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        assertSame(role, service.findByName("ADMIN"));
    }

    @Test
    void addRoleAddsRole() {
        User user = userWithRoles();
        Role role = new Role();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        service.addRole(1L, "ADMIN");
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void addingSameRoleTwiceDoesNotCreateDuplicate() {
        User user = userWithRoles();
        Role role = new Role();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        service.addRole(1L, "ADMIN");
        service.addRole(1L, "ADMIN");

        assertEquals(1, user.getRoles().size());
    }

    @Test
    void removingNonAdminRoleStillWorks() {
        User user = userWithRoles();
        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getId()).thenReturn(5L);
        user.getRoles().add(role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(role));

        service.removeRole(1L, "EMPLOYEE", 2L);
        assertEquals(0, user.getRoles().size());
        verify(userRepository, never()).countDistinctByRolesName("ADMIN");
    }

    @Test
    void adminCannotRemoveOwnAdminRole() {
        InvalidOperationException error = assertThrows(InvalidOperationException.class,
                () -> service.removeRole(1L, "ADMIN", 1L));

        assertEquals("Не можна видалити роль ADMIN у власного облікового запису", error.getMessage());
        verify(roleRepository, never()).findByNameForUpdate("ADMIN");
    }

    @Test
    void cannotRemoveAdminRoleFromLastAdmin() {
        Role admin = role(5L);
        User target = userWithRoles(admin);
        when(roleRepository.findByNameForUpdate("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countDistinctByRolesName("ADMIN")).thenReturn(1L);

        InvalidOperationException error = assertThrows(InvalidOperationException.class,
                () -> service.removeRole(2L, "ADMIN", 1L));

        assertEquals("Не можна видалити роль ADMIN в останнього адміністратора", error.getMessage());
        assertEquals(1, target.getRoles().size());
    }

    @Test
    void adminCanRemoveAdminRoleFromAnotherAdminWhenMultipleAdminsExist() {
        Role admin = role(5L);
        User target = userWithRoles(admin);
        when(roleRepository.findByNameForUpdate("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countDistinctByRolesName("ADMIN")).thenReturn(2L);

        service.removeRole(2L, "ADMIN", 1L);

        assertEquals(0, target.getRoles().size());
    }

    private User userWithRoles(Role... roles) {
        User user = new User();
        user.setRoles(new HashSet<>(java.util.List.of(roles)));
        return user;
    }

    private Role role(Long id) {
        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getId()).thenReturn(id);
        return role;
    }
}
